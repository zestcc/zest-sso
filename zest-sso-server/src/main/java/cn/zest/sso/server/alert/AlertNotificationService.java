package cn.zest.sso.server.alert;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.plugin.alert.AlertChannelAdapter;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.vo.AlertChannelConfigVO;
import cn.zest.sso.server.plugin.AlertChannelConfigService;
import cn.zest.sso.server.service.WebhookDeliveryService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotificationService {

    private final SsoProperties ssoProperties;
    private final AlertChannelRegistry alertChannelRegistry;
    private final AlertChannelConfigService alertChannelConfigService;
    private final WebhookDeliveryService webhookDeliveryService;
    private final ObjectMapper objectMapper;

    @Async("taskExecutor")
    public void publish(AuditEventType eventType, String actor, String target, String detail) {
        publishLegacyWebhooks(eventType, actor, target, detail);
        publishAlertChannels(eventType, actor, target, detail);
    }

    private void publishLegacyWebhooks(AuditEventType eventType, String actor, String target, String detail) {
        SsoProperties.Webhooks webhooks = ssoProperties.getWebhooks();
        if (!ssoProperties.getModules().isWebhooks() || !webhooks.isEnabled()
                || webhooks.getEndpoints() == null || webhooks.getEndpoints().isEmpty()) {
            return;
        }
        List<String> allowed = webhooks.getEvents();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(eventType.getCode())) {
            return;
        }
        String body = serialize(eventType, actor, target, detail);
        for (String endpoint : webhooks.getEndpoints()) {
            if (StringUtils.hasText(endpoint)) {
                webhookDeliveryService.enqueue(eventType.getCode(), endpoint, body);
            }
        }
    }

    private void publishAlertChannels(AuditEventType eventType, String actor, String target, String detail) {
        if (!ssoProperties.getModules().isAlerts()) {
            return;
        }
        Map<String, Object> payload = buildPayload(eventType, actor, target, detail);
        List<AlertChannelConfigVO> dbChannels = alertChannelConfigService.listEnabled();
        if (!dbChannels.isEmpty()) {
            publishFromDatabase(eventType, payload, dbChannels);
            return;
        }
        publishFromYaml(eventType, payload);
    }

    private void publishFromDatabase(AuditEventType eventType, Map<String, Object> payload,
                                     List<AlertChannelConfigVO> channels) {
        for (AlertChannelConfigVO channel : channels) {
            if (channel == null || !StringUtils.hasText(channel.getChannelKey())) {
                continue;
            }
            if (channel.getEvents() != null && !channel.getEvents().isEmpty()
                    && !channel.getEvents().contains(eventType.getCode())) {
                continue;
            }
            if (!alertChannelRegistry.hasChannel(channel.getChannelKey())) {
                log.warn("告警通道 {} 未安装", channel.getChannelKey());
                continue;
            }
            try {
                AlertChannelAdapter adapter = alertChannelRegistry.resolve(channel.getChannelKey());
                if (!adapter.supportsEvent(eventType.getCode())) {
                    continue;
                }
                Map<String, String> config = channel.getConfig() != null ? channel.getConfig() : Map.of();
                adapter.send(eventType.getCode(), payload, config);
            } catch (Exception ex) {
                log.warn("告警通道 {} 发送失败: {}", channel.getChannelKey(), ex.getMessage());
            }
        }
    }

    private void publishFromYaml(AuditEventType eventType, Map<String, Object> payload) {
        if (!ssoProperties.getAlerts().isEnabled()) {
            return;
        }
        for (SsoProperties.Alerts.ChannelBinding binding : ssoProperties.getAlerts().getChannels()) {
            if (binding == null || !binding.isEnabled() || !StringUtils.hasText(binding.getChannelKey())) {
                continue;
            }
            if (binding.getEvents() != null && !binding.getEvents().isEmpty()
                    && !binding.getEvents().contains(eventType.getCode())) {
                continue;
            }
            if (!alertChannelRegistry.hasChannel(binding.getChannelKey())) {
                continue;
            }
            try {
                AlertChannelAdapter adapter = alertChannelRegistry.resolve(binding.getChannelKey());
                if (!adapter.supportsEvent(eventType.getCode())) {
                    continue;
                }
                Map<String, String> config = binding.getConfig() != null ? binding.getConfig() : Map.of();
                adapter.send(eventType.getCode(), payload, config);
            } catch (Exception ex) {
                log.warn("告警通道 {} 发送失败: {}", binding.getChannelKey(), ex.getMessage());
            }
        }
    }

    private Map<String, Object> buildPayload(AuditEventType eventType, String actor, String target, String detail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", eventType.getCode());
        payload.put("eventLabel", eventType.getLabel());
        payload.put("timestamp", Instant.now().toString());
        payload.put("actor", actor);
        payload.put("target", target);
        payload.put("detail", detail);
        return payload;
    }

    private String serialize(AuditEventType eventType, String actor, String target, String detail) {
        try {
            return objectMapper.writeValueAsString(buildPayload(eventType, actor, target, detail));
        } catch (Exception ex) {
            log.warn("Webhook 序列化失败: {}", ex.getMessage());
            return "{}";
        }
    }
}
