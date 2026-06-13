package cn.zest.sso.client;

/**
 * RP 侧 Back-Channel Logout 回调：吊销本地会话/令牌。
 */
@FunctionalInterface
public interface SsoLogoutHandler {

    /**
     * @param principal SSO 用户名（preferred_username 或 sub）
     */
    void onBackchannelLogout(String principal);
}
