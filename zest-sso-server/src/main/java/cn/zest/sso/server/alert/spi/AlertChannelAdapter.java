package cn.zest.sso.server.alert.spi;

import java.util.Map;

/**
 * 告警通知通道（HTTP Webhook / 钉钉机器人 / 企微机器人等）。
 */
public interface AlertChannelAdapter {

    String channelKey();

    AlertChannelDescriptor descriptor();

    boolean supportsEvent(String eventCode);

    void send(String eventCode, Map<String, Object> payload, Map<String, String> config);
}
