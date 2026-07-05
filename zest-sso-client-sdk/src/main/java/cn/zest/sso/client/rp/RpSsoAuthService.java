package cn.zest.sso.client.rp;

/**
 * RP SSO 认证门面 — 委托 {@link RpSsoProviderRegistry} 选择具体提供方。
 */
public class RpSsoAuthService {

    private final RpSsoProperties properties;
    private final RpSsoProviderRegistry providerRegistry;

    public RpSsoAuthService(RpSsoProperties properties, RpSsoProviderRegistry providerRegistry) {
        this.properties = properties;
        this.providerRegistry = providerRegistry;
    }

    public RpSsoPublicConfig getConfig() {
        return providerRegistry.resolve().buildPublicConfig(properties);
    }

    public RpSsoAuthorizeInfo buildAuthorizeUrl() {
        return providerRegistry.resolve().buildAuthorizeUrl(properties);
    }

    public String buildLogoutUrl() {
        if (!properties.isEnabled()) {
            return null;
        }
        return providerRegistry.resolve().buildLogoutUrl(properties);
    }

    public RpSsoCallbackResult handleCallback(RpSsoCallbackRequest request) {
        return providerRegistry.resolve().handleCallback(request, properties);
    }

    public RpSsoCallbackResult exchangeIdToken(String idToken) {
        return providerRegistry.resolve().exchangeIdToken(idToken, properties);
    }
}
