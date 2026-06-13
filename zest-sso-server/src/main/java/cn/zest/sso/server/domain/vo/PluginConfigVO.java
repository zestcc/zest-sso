package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class PluginConfigVO {
    private String pluginKey;
    private String pluginName;
    private String category;
    private boolean installed;
    private boolean enabled;
    private boolean configured;
    private Map<String, String> configHints;
    private Map<String, String> config;
}
