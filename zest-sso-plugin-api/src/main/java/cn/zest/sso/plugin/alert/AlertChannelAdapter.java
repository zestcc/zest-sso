package cn.zest.sso.plugin.alert;

import cn.zest.sso.plugin.PluggablePlugin;

import java.util.Map;

public interface AlertChannelAdapter extends PluggablePlugin {

    @Override
    default String category() {
        return "alert";
    }

    String channelKey();

    @Override
    default String pluginKey() {
        return channelKey();
    }

    AlertChannelDescriptor descriptor();

    boolean supportsEvent(String eventCode);

    void send(String eventCode, Map<String, Object> payload, Map<String, String> config);
}
