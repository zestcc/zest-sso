package cn.zest.sso.server.modules.spi;

import java.util.Map;

/**
 * Admin 控制台展示的可选模块描述。
 */
public record OptionalModuleDescriptor(
        String key,
        String name,
        String description,
        String category,
        boolean enabled,
        boolean configurable,
        Map<String, String> configHints
) {
}
