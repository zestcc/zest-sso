package cn.zest.sso.server.job;

import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogArchiveJob {

    private final AuditService auditService;
    private final SsoProperties ssoProperties;

    @Scheduled(cron = "${zest.sso.audit.archive-cron:0 0 3 * * ?}")
    public void purgeExpiredLogs() {
        SsoProperties.Audit audit = ssoProperties.getAudit();
        if (!audit.isArchiveEnabled()) {
            return;
        }
        int removed = auditService.purgeOlderThanDays(audit.getRetentionDays());
        if (removed > 0) {
            log.info("审计日志归档：删除 {} 条超过 {} 天的记录", removed, audit.getRetentionDays());
        }
    }
}
