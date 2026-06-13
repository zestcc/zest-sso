package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.server.config.SsoProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IAM 事件 Webhook（对标 Auth0 Log Stream / Okta Event Hooks）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventPublisher {

    private final SsoProperties ssoProperties;
    private final WebhookDeliveryService webhookDeliveryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async("taskExecutor")
    public void publish(AuditEventType eventType, String actor, String target, String detail) {
        SsoProperties.Webhooks webhooks = ssoProperties.getWebhooks();
        if (!webhooks.isEnabled() || webhooks.getEndpoints() == null || webhooks.getEndpoints().isEmpty()) {
            return;
        }
        List<String> allowed = webhooks.getEvents();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(eventType.getCode())) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", eventType.getCode());
        payload.put("eventLabel", eventType.getLabel());
        payload.put("timestamp", Instant.now().toString());
        payload.put("actor", actor);
        payload.put("target", target);
        payload.put("detail", detail);
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("Webhook 序列化失败: {}", ex.getMessage());
            return;
        }
        for (String endpoint : webhooks.getEndpoints()) {
            if (!StringUtils.hasText(endpoint)) {
                continue;
            }
            webhookDeliveryService.enqueue(eventType.getCode(), endpoint, body);
        }
    }
}
