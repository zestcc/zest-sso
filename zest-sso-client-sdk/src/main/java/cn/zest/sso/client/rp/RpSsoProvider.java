package cn.zest.sso.client.rp;

/**
 * RP 侧 SSO 提供方 SPI — 支持 ZestSSO、通用 OIDC 等可插拔实现。
 */
public interface RpSsoProvider {

    String providerId();

    RpSsoPublicConfig buildPublicConfig(RpSsoProperties props);

    RpSsoAuthorizeInfo buildAuthorizeUrl(RpSsoProperties props);

    String buildLogoutUrl(RpSsoProperties props);

    RpSsoCallbackResult handleCallback(RpSsoCallbackRequest request, RpSsoProperties props);

    default RpSsoCallbackResult exchangeIdToken(String idToken, RpSsoProperties props) {
        throw new RpSsoException("NOT_SUPPORTED",
                "exchangeIdToken not supported for provider " + providerId());
    }
}
