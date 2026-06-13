package cn.zest.sso.server.alert.spi;

import java.util.Map;

public record AlertChannelDescriptor(
        String key,
        String name,
        String description,
        boolean enabled,
        Map<String, String> configHints
) {
}
