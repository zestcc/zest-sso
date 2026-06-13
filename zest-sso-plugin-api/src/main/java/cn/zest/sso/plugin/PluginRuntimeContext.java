package cn.zest.sso.plugin;

import java.util.Map;

/**
 * 插件运行时上下文 — 由服务端注入 DB/YAML 合并后的配置。
 */
public interface PluginRuntimeContext {

    boolean isPluginEnabled(String pluginKey);

    Map<String, String> getPluginConfig(String pluginKey);
}
