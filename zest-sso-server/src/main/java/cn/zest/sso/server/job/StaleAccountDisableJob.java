package cn.zest.sso.server.job;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.enums.UserStatus;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.mapper.SsoUserMapper;
import cn.zest.sso.server.service.AuditService;
import cn.zest.sso.server.service.WebhookEventPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaleAccountDisableJob {

    private final SsoUserMapper userMapper;
    private final SsoProperties ssoProperties;
    private final AuditService auditService;
    private final WebhookEventPublisher webhookEventPublisher;

    @Scheduled(cron = "${zest.sso.governance.stale-account-cron:0 30 2 * * ?}")
    public void disableStaleAccounts() {
        SsoProperties.Governance governance = ssoProperties.getGovernance();
        if (!governance.isStaleAccountDisableEnabled() || governance.getStaleAccountDays() <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(governance.getStaleAccountDays());
        List<SsoUser> staleUsers = userMapper.selectList(new LambdaQueryWrapper<SsoUser>()
                .eq(SsoUser::getStatus, UserStatus.ACTIVE.getCode())
                .and(w -> w.isNull(SsoUser::getLastLoginAt).or().lt(SsoUser::getLastLoginAt, cutoff))
                .ne(SsoUser::getIsSuperAdmin, 1));
        for (SsoUser user : staleUsers) {
            SsoUser update = new SsoUser();
            update.setId(user.getId());
            update.setStatus(UserStatus.DISABLED.getCode());
            update.setInactiveDisabledAt(LocalDateTime.now());
            userMapper.updateById(update);
            auditService.log(AuditEventType.USER_DISABLE, "system", user.getUsername(),
                    null, null, null, "闲置账号自动禁用");
            webhookEventPublisher.publish(AuditEventType.USER_DISABLE, "system", user.getUsername(),
                    "闲置超过 " + governance.getStaleAccountDays() + " 天");
            log.info("闲置账号已禁用: {}", user.getUsername());
        }
    }
}
