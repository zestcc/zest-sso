package cn.zest.sso.server.service;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoBackchannelLogoutDelivery;
import cn.zest.sso.server.domain.entity.SsoOAuthClient;
import cn.zest.sso.server.domain.mapper.SsoBackchannelLogoutDeliveryMapper;
import cn.zest.sso.server.domain.vo.BackchannelDeliveryVO;
import cn.zest.sso.server.metrics.SsoMetrics;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackchannelLogoutDeliveryService {

    private final SsoBackchannelLogoutDeliveryMapper deliveryMapper;
    private final LogoutTokenService logoutTokenService;
    private final SsoProperties ssoProperties;
    private final SsoMetrics ssoMetrics;
    @Qualifier("logoutRestTemplate")
    private final RestTemplate logoutRestTemplate;

    @Transactional(rollbackFor = Exception.class)
    public void enqueueDeliveries(List<SsoOAuthClient> targets, String principalName) {
        for (SsoOAuthClient client : targets) {
            if (!StringUtils.hasText(client.getBackchannelLogoutUri())) {
                continue;
            }
            SsoBackchannelLogoutDelivery delivery = new SsoBackchannelLogoutDelivery();
            delivery.setPrincipalName(principalName);
            delivery.setClientId(client.getClientId());
            delivery.setBackchannelUri(client.getBackchannelLogoutUri());
            delivery.setStatus(SsoBackchannelLogoutDelivery.STATUS_PENDING);
            delivery.setAttemptCount(0);
            delivery.setNextRetryAt(LocalDateTime.now());
            deliveryMapper.insert(delivery);
            deliver(delivery);
        }
    }

    public void deliver(SsoBackchannelLogoutDelivery delivery) {
        if (delivery == null || SsoBackchannelLogoutDelivery.STATUS_SUCCESS.equals(delivery.getStatus())
                || SsoBackchannelLogoutDelivery.STATUS_DEAD.equals(delivery.getStatus())) {
            return;
        }
        int attempt = delivery.getAttemptCount() == null ? 0 : delivery.getAttemptCount();
        delivery.setAttemptCount(attempt + 1);
        try {
            String logoutToken = logoutTokenService.createLogoutToken(delivery.getPrincipalName(), delivery.getClientId());
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("logout_token", logoutToken);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            ResponseEntity<String> response = logoutRestTemplate.postForEntity(
                    delivery.getBackchannelUri(), new HttpEntity<>(body, headers), String.class);
            delivery.setLastHttpStatus(response.getStatusCode().value());
            if (response.getStatusCode().is2xxSuccessful()) {
                delivery.setStatus(SsoBackchannelLogoutDelivery.STATUS_SUCCESS);
                delivery.setLastError(null);
                delivery.setNextRetryAt(null);
                ssoMetrics.recordBackchannelLogoutSuccess();
                log.info("Back-Channel Logout 成功: client={}, principal={}", delivery.getClientId(), delivery.getPrincipalName());
            } else {
                markFailure(delivery, "HTTP " + response.getStatusCode().value());
            }
        } catch (Exception ex) {
            delivery.setLastHttpStatus(null);
            markFailure(delivery, ex.getMessage());
        }
        deliveryMapper.updateById(delivery);
    }

    public SsoBackchannelLogoutDelivery findByIdOrThrow(Long id) {
        SsoBackchannelLogoutDelivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "Back-Channel 投递记录不存在");
        }
        return delivery;
    }

    public int retryDueDeliveries() {
        LocalDateTime now = LocalDateTime.now();
        List<SsoBackchannelLogoutDelivery> due = deliveryMapper.selectList(new LambdaQueryWrapper<SsoBackchannelLogoutDelivery>()
                .eq(SsoBackchannelLogoutDelivery::getStatus, SsoBackchannelLogoutDelivery.STATUS_FAILED)
                .le(SsoBackchannelLogoutDelivery::getNextRetryAt, now)
                .last("LIMIT 50"));
        for (SsoBackchannelLogoutDelivery delivery : due) {
            deliver(delivery);
        }
        return due.size();
    }

    public Page<BackchannelDeliveryVO> page(int page, int size, String status, String principalName) {
        LambdaQueryWrapper<SsoBackchannelLogoutDelivery> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(SsoBackchannelLogoutDelivery::getStatus, status);
        }
        if (StringUtils.hasText(principalName)) {
            wrapper.like(SsoBackchannelLogoutDelivery::getPrincipalName, principalName);
        }
        wrapper.orderByDesc(SsoBackchannelLogoutDelivery::getCreateTime);
        Page<SsoBackchannelLogoutDelivery> result = deliveryMapper.selectPage(new Page<>(page, size), wrapper);
        Page<BackchannelDeliveryVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVo).toList());
        return voPage;
    }

    private void markFailure(SsoBackchannelLogoutDelivery delivery, String error) {
        ssoMetrics.recordBackchannelLogoutFailure();
        delivery.setLastError(truncate(error, 500));
        int maxRetries = ssoProperties.getBackchannel().getMaxRetries();
        if (delivery.getAttemptCount() >= maxRetries) {
            delivery.setStatus(SsoBackchannelLogoutDelivery.STATUS_DEAD);
            delivery.setNextRetryAt(null);
            log.warn("Back-Channel Logout 进入死信: client={}, principal={}, attempts={}",
                    delivery.getClientId(), delivery.getPrincipalName(), delivery.getAttemptCount());
        } else {
            delivery.setStatus(SsoBackchannelLogoutDelivery.STATUS_FAILED);
            delivery.setNextRetryAt(LocalDateTime.now().plusSeconds(ssoProperties.getBackchannel().getRetryDelaySeconds()));
            log.warn("Back-Channel Logout 失败将重试: client={}, error={}", delivery.getClientId(), delivery.getLastError());
        }
    }

    private BackchannelDeliveryVO toVo(SsoBackchannelLogoutDelivery delivery) {
        return BackchannelDeliveryVO.builder()
                .id(delivery.getId())
                .principalName(delivery.getPrincipalName())
                .clientId(delivery.getClientId())
                .backchannelUri(delivery.getBackchannelUri())
                .status(delivery.getStatus())
                .attemptCount(delivery.getAttemptCount())
                .lastHttpStatus(delivery.getLastHttpStatus())
                .lastError(delivery.getLastError())
                .nextRetryAt(delivery.getNextRetryAt())
                .createTime(delivery.getCreateTime())
                .build();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
