package cn.zest.sso.plugin;

import java.util.Map;

/**
 * 可插拔插件标记 — 独立 Maven 模块实现本接口并注册为 Spring Bean。
 * <p>未加入 classpath 的插件不会被打包，也不会出现在运行时。
 */
public interface PluggablePlugin {

    String pluginKey();

    String pluginName();

    String category();

    /** Admin 配置表单字段提示 */
    Map<String, String> configFieldHints();

    /** 插件 JAR 是否已加载（由实现类存在即表示已安装） */
    default boolean installed() {
        return true;
    }
}
