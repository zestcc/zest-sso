package cn.zest.sso.plugin.alert;

import java.util.Map;

public record AlertChannelDescriptor(
        String key,
        String name,
        String description,
        boolean installed,
        Map<String, String> configHints
) {
}
