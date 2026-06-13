package cn.zest.sso.server.modules.spi;

/**
 * 可插拔可选模块 — 通过配置开关启用/禁用，Admin 可查询能力清单。
 */
public interface OptionalModule {

    String moduleKey();

    OptionalModuleDescriptor descriptor();

    /** 当前环境是否已启用（配置 + 必要凭据） */
    boolean isEnabled();
}
