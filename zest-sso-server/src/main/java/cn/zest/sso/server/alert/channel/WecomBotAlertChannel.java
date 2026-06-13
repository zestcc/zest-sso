package cn.zest.sso.server.alert.channel;

import cn.zest.sso.plugin.alert.AlertChannelAdapter;
import cn.zest.sso.plugin.alert.AlertChannelDescriptor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WecomBotAlertChannel implements AlertChannelAdapter {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String channelKey() {
        return "wecom-bot";
    }

    @Override
    public String pluginName() {
        return "企业微信机器人";
    }

    @Override
    public Map<String, String> configFieldHints() {
        return Map.of("webhookUrl", "机器人 Webhook URL");
    }

    @Override
    public AlertChannelDescriptor descriptor() {
        return new AlertChannelDescriptor(
                channelKey(), pluginName(), "IAM 事件推送至企微群机器人",
                true, configFieldHints());
    }

    @Override
    public boolean supportsEvent(String eventCode) {
        return true;
    }

    @Override
    public void send(String eventCode, Map<String, Object> payload, Map<String, String> config) {
        String webhookUrl = config != null ? config.get("webhookUrl") : null;
        if (!StringUtils.hasText(webhookUrl)) {
            throw new IllegalArgumentException("wecom-bot 缺少 config.webhookUrl");
        }
        String content = "[ZestSSO] " + eventCode + " | "
                + payload.get("actor") + " -> " + payload.get("target") + " | "
                + payload.get("detail");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "text");
        body.put("text", Map.of("content", content));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity(webhookUrl, new HttpEntity<>(body, headers), String.class);
    }
}
