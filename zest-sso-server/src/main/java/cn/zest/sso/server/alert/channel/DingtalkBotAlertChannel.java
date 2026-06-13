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
public class DingtalkBotAlertChannel implements AlertChannelAdapter {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String channelKey() {
        return "dingtalk-bot";
    }

    @Override
    public String pluginName() {
        return "钉钉机器人";
    }

    @Override
    public Map<String, String> configFieldHints() {
        return Map.of("webhookUrl", "机器人 Webhook URL", "secret", "可选加签 secret");
    }

    @Override
    public AlertChannelDescriptor descriptor() {
        return new AlertChannelDescriptor(
                channelKey(), pluginName(), "IAM 事件推送至钉钉群机器人",
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
            throw new IllegalArgumentException("dingtalk-bot 缺少 config.webhookUrl");
        }
        String text = formatText(eventCode, payload);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msgtype", "text");
        body.put("text", Map.of("content", text));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity(webhookUrl, new HttpEntity<>(body, headers), String.class);
    }

    private String formatText(String eventCode, Map<String, Object> payload) {
        return "[ZestSSO] " + eventCode + "\n"
                + "时间: " + payload.get("timestamp") + "\n"
                + "操作者: " + payload.get("actor") + "\n"
                + "对象: " + payload.get("target") + "\n"
                + "详情: " + payload.get("detail");
    }
}
