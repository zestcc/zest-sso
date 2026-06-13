package cn.zest.sso.server.alert;

import cn.zest.sso.plugin.alert.AlertChannelAdapter;
import cn.zest.sso.plugin.alert.AlertChannelDescriptor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AlertChannelRegistry {

    private final Map<String, AlertChannelAdapter> channelsByKey;

    public AlertChannelRegistry(List<AlertChannelAdapter> adapters) {
        this.channelsByKey = adapters.stream()
                .collect(Collectors.toMap(AlertChannelAdapter::channelKey, Function.identity(), (a, b) -> a));
    }

    public List<AlertChannelDescriptor> listDescriptors() {
        return channelsByKey.values().stream()
                .map(AlertChannelAdapter::descriptor)
                .sorted(Comparator.comparing(AlertChannelDescriptor::key))
                .toList();
    }

    public boolean hasChannel(String channelKey) {
        return channelsByKey.containsKey(channelKey);
    }

    public AlertChannelAdapter resolve(String channelKey) {
        AlertChannelAdapter adapter = channelsByKey.get(channelKey);
        if (adapter == null) {
            throw new cn.zest.sso.common.exception.SsoException(
                    cn.zest.sso.common.exception.ErrorCode.BAD_REQUEST, "未知告警通道: " + channelKey);
        }
        return adapter;
    }
}
