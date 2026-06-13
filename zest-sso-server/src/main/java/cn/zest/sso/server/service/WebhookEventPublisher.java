package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.server.alert.AlertNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 兼容层 — 委托给 {@link AlertNotificationService} 统一告警/Webhook 投递。
 */
@Service
@RequiredArgsConstructor
public class WebhookEventPublisher {

    private final AlertNotificationService alertNotificationService;

    public void publish(AuditEventType eventType, String actor, String target, String detail) {
        alertNotificationService.publish(eventType, actor, target, detail);
    }
}
