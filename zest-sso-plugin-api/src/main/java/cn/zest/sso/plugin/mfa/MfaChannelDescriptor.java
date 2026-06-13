package cn.zest.sso.plugin.mfa;

import java.util.Map;

public record MfaChannelDescriptor(
        String key,
        String name,
        String description,
        boolean installed,
        boolean enabled,
        boolean configured,
        Map<String, String> configHints
) {
}
