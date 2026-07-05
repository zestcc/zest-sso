package cn.zest.sso.client.rp.oidc;

/**
 * OIDC Discovery 解析后的端点集合。
 */
public record OidcEndpoints(
        String issuer,
        String authorizationEndpoint,
        String tokenEndpoint,
        String jwksUri,
        String endSessionEndpoint
) {
}
