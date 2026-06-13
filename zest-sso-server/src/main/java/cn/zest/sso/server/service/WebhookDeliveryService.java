package cn.zest.sso.server.service;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoWebhookDelivery;
import cn.zest.sso.server.domain.mapper.SsoWebhookDeliveryMapper;
import cn.zest.sso.server.domain.vo.WebhookDeliveryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDeliveryService {

    private final SsoWebhookDeliveryMapper deliveryMapper;
    private final SsoProperties ssoProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional(rollbackFor = Exception.class)
    public void enqueue(String eventType, String endpointUrl, String payloadJson) {
        SsoWebhookDelivery delivery = new SsoWebhookDelivery();
        delivery.setEventType(eventType);
        delivery.setEndpointUrl(endpointUrl);
        delivery.setPayloadJson(payloadJson);
        delivery.setStatus(SsoWebhookDelivery.STATUS_PENDING);
        delivery.setAttemptCount(0);
        delivery.setNextRetryAt(LocalDateTime.now());
        deliveryMapper.insert(delivery);
        deliver(delivery);
    }

    public void deliver(SsoWebhookDelivery delivery) {
        if (delivery == null || SsoWebhookDelivery.STATUS_SUCCESS.equals(delivery.getStatus())
                || SsoWebhookDelivery.STATUS_DEAD.equals(delivery.getStatus())) {
            return;
        }
        int attempt = delivery.getAttemptCount() == null ? 0 : delivery.getAttemptCount();
        delivery.setAttemptCount(attempt + 1);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            String secret = ssoProperties.getWebhooks().getSigningSecret();
            if (StringUtils.hasText(secret)) {
                headers.set("X-Zest-Sso-Signature", sign(delivery.getPayloadJson(), secret));
            }
            ResponseEntity<String> response = restTemplate.postForEntity(
                    delivery.getEndpointUrl(),
                    new HttpEntity<>(delivery.getPayloadJson(), headers),
                    String.class);
            delivery.setLastHttpStatus(response.getStatusCode().value());
            if (response.getStatusCode().is2xxSuccessful()) {
                delivery.setStatus(SsoWebhookDelivery.STATUS_SUCCESS);
                delivery.setLastError(null);
                delivery.setNextRetryAt(null);
            } else {
                markFailed(delivery, "HTTP " + response.getStatusCode().value());
            }
        } catch (Exception ex) {
            markFailed(delivery, ex.getMessage());
        }
        deliveryMapper.updateById(delivery);
    }

    public void retryDueDeliveries() {
        SsoProperties.Webhooks webhooks = ssoProperties.getWebhooks();
        if (!webhooks.isEnabled()) {
            return;
        }
        List<SsoWebhookDelivery> due = deliveryMapper.selectList(new LambdaQueryWrapper<SsoWebhookDelivery>()
                .in(SsoWebhookDelivery::getStatus, SsoWebhookDelivery.STATUS_PENDING, SsoWebhookDelivery.STATUS_FAILED)
                .le(SsoWebhookDelivery::getNextRetryAt, LocalDateTime.now())
                .last("LIMIT 50"));
        for (SsoWebhookDelivery delivery : due) {
            deliver(delivery);
        }
    }

    public Page<WebhookDeliveryVO> page(int page, int size, String status, String eventType) {
        LambdaQueryWrapper<SsoWebhookDelivery> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(SsoWebhookDelivery::getStatus, status);
        }
        if (StringUtils.hasText(eventType)) {
            wrapper.eq(SsoWebhookDelivery::getEventType, eventType);
        }
        wrapper.orderByDesc(SsoWebhookDelivery::getCreateTime);
        Page<SsoWebhookDelivery> result = deliveryMapper.selectPage(new Page<>(page, size), wrapper);
        Page<WebhookDeliveryVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVo).toList());
        return voPage;
    }

    public SsoWebhookDelivery findByIdOrThrow(Long id) {
        SsoWebhookDelivery delivery = deliveryMapper.selectById(id);
        if (delivery == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "Webhook 投递记录不存在");
        }
        return delivery;
    }

    private void markFailed(SsoWebhookDelivery delivery, String error) {
        delivery.setLastError(truncate(error));
        SsoProperties.Webhooks webhooks = ssoProperties.getWebhooks();
        int maxRetries = webhooks.getMaxRetries();
        if (delivery.getAttemptCount() >= maxRetries) {
            delivery.setStatus(SsoWebhookDelivery.STATUS_DEAD);
            delivery.setNextRetryAt(null);
            log.warn("Webhook 投递进入死信: id={}, endpoint={}", delivery.getId(), delivery.getEndpointUrl());
        } else {
            delivery.setStatus(SsoWebhookDelivery.STATUS_FAILED);
            delivery.setNextRetryAt(LocalDateTime.now().plusSeconds(webhooks.getRetryDelaySeconds()));
        }
    }

    private WebhookDeliveryVO toVo(SsoWebhookDelivery delivery) {
        return WebhookDeliveryVO.builder()
                .id(delivery.getId())
                .eventType(delivery.getEventType())
                .endpointUrl(delivery.getEndpointUrl())
                .status(delivery.getStatus())
                .attemptCount(delivery.getAttemptCount())
                .lastHttpStatus(delivery.getLastHttpStatus())
                .lastError(delivery.getLastError())
                .nextRetryAt(delivery.getNextRetryAt())
                .createTime(delivery.getCreateTime())
                .build();
    }

    private static String sign(String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("HMAC 签名失败", ex);
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
