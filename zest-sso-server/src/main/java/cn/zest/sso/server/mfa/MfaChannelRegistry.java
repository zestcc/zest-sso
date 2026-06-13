package cn.zest.sso.server.mfa;

import cn.zest.sso.plugin.mfa.MfaChannelAdapter;
import cn.zest.sso.plugin.mfa.MfaChannelDescriptor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MfaChannelRegistry {

    private final Map<String, MfaChannelAdapter> channelsByKey;

    public MfaChannelRegistry(List<MfaChannelAdapter> adapters) {
        this.channelsByKey = adapters.stream()
                .collect(Collectors.toMap(MfaChannelAdapter::channelKey, Function.identity(), (a, b) -> a));
    }

    public List<MfaChannelDescriptor> listDescriptors() {
        return channelsByKey.values().stream()
                .map(MfaChannelAdapter::descriptor)
                .sorted(Comparator.comparing(MfaChannelDescriptor::key))
                .toList();
    }

    public boolean hasChannel(String channelKey) {
        return channelsByKey.containsKey(channelKey);
    }

    public MfaChannelAdapter resolve(String channelKey) {
        MfaChannelAdapter adapter = channelsByKey.get(channelKey);
        if (adapter == null) {
            throw new cn.zest.sso.common.exception.SsoException(
                    cn.zest.sso.common.exception.ErrorCode.BAD_REQUEST, "未知 MFA 通道: " + channelKey);
        }
        return adapter;
    }

    public MfaChannelAdapter resolveEnabled(String channelKey) {
        MfaChannelAdapter adapter = resolve(channelKey);
        if (!adapter.isEnabled()) {
            throw new cn.zest.sso.common.exception.SsoException(
                    cn.zest.sso.common.exception.ErrorCode.BAD_REQUEST, "MFA 通道未启用: " + channelKey);
        }
        return adapter;
    }
}
