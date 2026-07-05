package cn.zest.sso.client.rp;

/**
 * 将任意产品 SSO 配置适配为 {@link RpSsoProperties}。
 */
public final class RpSsoPropertiesBridge {

    private RpSsoPropertiesBridge() {
    }

    public static RpSsoProperties wrap(RpSsoProperties source) {
        return source;
    }

    public static RpSsoProperties from(AdminSsoConfigLike config) {
        return new RpSsoProperties() {
            @Override
            public boolean isEnabled() {
                return config.isEnabled();
            }

            @Override
            public String getProvider() {
                return config.getProvider();
            }

            @Override
            public String getDisplayName() {
                return config.getDisplayName();
            }

            @Override
            public String getIssuer() {
                return config.getIssuer();
            }

            @Override
            public String getDiscoveryUri() {
                return config.getDiscoveryUri();
            }

            @Override
            public String getClientId() {
                return config.getClientId();
            }

            @Override
            public String getClientSecret() {
                return config.getClientSecret();
            }

            @Override
            public String getRedirectUri() {
                return config.getRedirectUri();
            }

            @Override
            public String getPostLogoutRedirectUri() {
                return config.getPostLogoutRedirectUri();
            }

            @Override
            public java.util.List<String> getScopes() {
                return config.getScopes();
            }

            @Override
            public String getJwksUri() {
                return config.getJwksUri();
            }

            @Override
            public String getUsernameClaim() {
                return config.getUsernameClaim();
            }

            @Override
            public String getRolesClaim() {
                return config.getRolesClaim();
            }

            @Override
            public boolean isZestSsoUseLogoutUrlApi() {
                return config.isZestSsoUseLogoutUrlApi();
            }

            @Override
            public String getZestSsoLogoutUrlApiPath() {
                return config.getZestSsoLogoutUrlApiPath();
            }
        };
    }

    /**
     * 各产品 Admin SSO 配置最小契约 — 用于 Bridge 适配。
     */
    public interface AdminSsoConfigLike {
        boolean isEnabled();
        String getProvider();
        String getDisplayName();
        String getIssuer();
        String getDiscoveryUri();
        String getClientId();
        String getClientSecret();
        String getRedirectUri();
        String getPostLogoutRedirectUri();
        java.util.List<String> getScopes();
        String getJwksUri();
        String getUsernameClaim();
        String getRolesClaim();
        boolean isZestSsoUseLogoutUrlApi();
        String getZestSsoLogoutUrlApiPath();
    }
}
