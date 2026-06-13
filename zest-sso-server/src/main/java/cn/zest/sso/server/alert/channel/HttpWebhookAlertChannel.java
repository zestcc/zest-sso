package cn.zest.sso.server.alert.channel;

import cn.zest.sso.server.alert.spi.AlertChannelAdapter;
import cn.zest.sso.server.alert.spi.AlertChannelDescriptor;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.service.WebhookDeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class HttpWebhookAlertChannel implements AlertChannelAdapter {

    private final SsoProperties ssoProperties;
    private final WebhookDeliveryService webhookDeliveryService;
    private final ObjectMapper objectMapper;

    @Override
    public String channelKey() {
        return "http-webhook";
    }

    @Override
    public AlertChannelDescriptor descriptor() {
        return new AlertChannelDescriptor(
                channelKey(), "HTTP Webhook", "通用 JSON Webhook（与 IAM 事件格式一致）",
                ssoProperties.getModules().isAlerts(),
                Map.of("url", "目标 URL", "signingSecret", "可选 HMAC 密钥"));
    }

    @Override
    public boolean supportsEvent(String eventCode) {
        return true;
    }

    @Override
    public void send(String eventCode, Map<String, Object> payload, Map<String, String> config) {
        String url = config != null ? config.get("url") : null;
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("http-webhook 缺少 config.url");
        }
        try {
            String body = objectMapper.writeValueAsString(payload);
            webhookDeliveryService.enqueue(eventCode, url, body);
        } catch (Exception ex) {
            throw new IllegalStateException("HTTP Webhook 序列化失败", ex);
        }
    }
}
