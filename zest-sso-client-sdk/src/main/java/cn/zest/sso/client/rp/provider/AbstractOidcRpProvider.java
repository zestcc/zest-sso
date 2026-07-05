package cn.zest.sso.client.rp.provider;

import cn.zest.sso.client.rp.*;
import cn.zest.sso.client.rp.oidc.OidcEndpointResolver;
import cn.zest.sso.client.rp.oidc.OidcEndpoints;
import cn.zest.sso.client.rp.oidc.OidcJwtValidator;
import cn.zest.sso.client.rp.oidc.OidcTokenClient;
import cn.zest.sso.client.rp.oidc.PkceUtils;
import cn.zest.sso.client.rp.store.RpPkceStore;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * OIDC + PKCE 通用 RP 流程基类。
 */
@RequiredArgsConstructor
public abstract class AbstractOidcRpProvider implements RpSsoProvider {

    protected final RpPkceStore pkceStore;
    protected final OidcEndpointResolver endpointResolver;
    protected final OidcTokenClient tokenClient;
    protected final OidcJwtValidator jwtValidator;

    protected abstract String displayName(RpSsoProperties props);

    @Override
    public RpSsoPublicConfig buildPublicConfig(RpSsoProperties props) {
        return RpSsoPublicConfig.builder()
                .enabled(props.isEnabled())
                .provider(providerId())
                .displayName(displayName(props))
                .issuer(props.getIssuer())
                .clientId(props.getClientId())
                .build();
    }

    @Override
    public RpSsoAuthorizeInfo buildAuthorizeUrl(RpSsoProperties props) {
        ensureEnabled(props);
        OidcEndpoints endpoints = endpointResolver.resolve(props);
        String state = PkceUtils.randomBase64Url(16);
        String codeVerifier = PkceUtils.randomBase64Url(32);
        String codeChallenge = PkceUtils.sha256Base64Url(codeVerifier);
        pkceStore.save(state, codeVerifier);

        String scopes = String.join(" ", props.getScopes());
        String url = endpoints.authorizationEndpoint()
                + "?response_type=code"
                + "&client_id=" + PkceUtils.urlEncode(props.getClientId())
                + "&redirect_uri=" + PkceUtils.urlEncode(props.getRedirectUri())
                + "&scope=" + PkceUtils.urlEncode(scopes)
                + "&state=" + PkceUtils.urlEncode(state)
                + "&code_challenge=" + PkceUtils.urlEncode(codeChallenge)
                + "&code_challenge_method=S256";

        return RpSsoAuthorizeInfo.builder()
                .authorizationUrl(url)
                .state(state)
                .build();
    }

    @Override
    public String buildLogoutUrl(RpSsoProperties props) {
        OidcEndpoints endpoints = endpointResolver.resolve(props);
        String redirect = props.getPostLogoutRedirectUri();
        if (StringUtils.hasText(endpoints.endSessionEndpoint())) {
            return endpoints.endSessionEndpoint()
                    + "?post_logout_redirect_uri=" + PkceUtils.urlEncode(redirect);
        }
        return props.getIssuer().replaceAll("/$", "")
                + "/connect/logout?post_logout_redirect_uri=" + PkceUtils.urlEncode(redirect);
    }

    @Override
    public RpSsoCallbackResult handleCallback(RpSsoCallbackRequest request, RpSsoProperties props) {
        ensureEnabled(props);
        String codeVerifier = pkceStore.consume(request.getState());
        if (!StringUtils.hasText(codeVerifier)) {
            throw new RpSsoException("INVALID_STATE", "无效的 state 或已过期");
        }
        OidcEndpoints endpoints = endpointResolver.resolve(props);
        String idToken = tokenClient.exchangeCodeForIdToken(request.getCode(), codeVerifier, endpoints, props);
        Claims claims = jwtValidator.parseAndValidate(idToken, endpoints, props);
        return RpSsoCallbackResult.builder()
                .providerId(providerId())
                .claims(claims)
                .idToken(idToken)
                .build();
    }

    @Override
    public RpSsoCallbackResult exchangeIdToken(String idToken, RpSsoProperties props) {
        ensureEnabled(props);
        if (!StringUtils.hasText(idToken)) {
            throw new RpSsoException("INVALID_TOKEN", "idToken 不能为空");
        }
        OidcEndpoints endpoints = endpointResolver.resolve(props);
        Claims claims = jwtValidator.parseAndValidate(idToken, endpoints, props);
        return RpSsoCallbackResult.builder()
                .providerId(providerId())
                .claims(claims)
                .idToken(idToken)
                .build();
    }

    protected void ensureEnabled(RpSsoProperties props) {
        if (!props.isEnabled()) {
            throw new RpSsoException("SSO_DISABLED", "SSO 未启用");
        }
    }
}
