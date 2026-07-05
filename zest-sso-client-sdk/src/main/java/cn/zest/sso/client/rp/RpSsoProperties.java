package cn.zest.sso.client.rp;

import java.util.List;

/**
 * Zest 生态 RP（各产品 Admin）SSO 统一配置契约。
 * <p>各产品 {@code @ConfigurationProperties} 实现本接口即可接入 {@link RpSsoAuthService}。</p>
 */
public interface RpSsoProperties {

    boolean isEnabled();

    /** 提供方：zest-sso | oidc | none */
    String getProvider();

    default String getDisplayName() {
        return null;
    }

    String getIssuer();

    default String getDiscoveryUri() {
        return null;
    }

    String getClientId();

    String getClientSecret();

    String getRedirectUri();

    default String getPostLogoutRedirectUri() {
        return null;
    }

    default List<String> getScopes() {
        return List.of("openid", "profile", "email", "roles", "tenant");
    }

    default String getJwksUri() {
        return null;
    }

    default String getUsernameClaim() {
        return "preferred_username";
    }

    default String getRolesClaim() {
        return "roles";
    }

    /** ZestSSO 专用：是否调用 logout-url API */
    default boolean isZestSsoUseLogoutUrlApi() {
        return true;
    }

    default String getZestSsoLogoutUrlApiPath() {
        return "/api/public/logout-url";
    }
}
