package cn.zest.sso.server.mfa.spi;

import java.util.Map;

public record MfaChannelDescriptor(
        String key,
        String name,
        String description,
        boolean enabled,
        boolean productionReady,
        Map<String, String> configHints
) {
}
