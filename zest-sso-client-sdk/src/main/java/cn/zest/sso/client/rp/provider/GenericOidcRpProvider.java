package cn.zest.sso.client.rp.provider;

import cn.zest.sso.client.rp.RpSsoProperties;
import cn.zest.sso.client.rp.oidc.OidcEndpointResolver;
import cn.zest.sso.client.rp.oidc.OidcJwtValidator;
import cn.zest.sso.client.rp.oidc.OidcTokenClient;
import cn.zest.sso.client.rp.store.RpPkceStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 通用 OIDC 提供方 — 适用于 Keycloak、Authing 等标准 IdP。
 */
public class GenericOidcRpProvider extends AbstractOidcRpProvider {

    public static final String PROVIDER_ID = "oidc";

    public GenericOidcRpProvider(RpPkceStore pkceStore,
                                 OidcEndpointResolver endpointResolver,
                                 OidcTokenClient tokenClient,
                                 OidcJwtValidator jwtValidator) {
        super(pkceStore, endpointResolver, tokenClient, jwtValidator);
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    protected String displayName(RpSsoProperties props) {
        return StringUtils.hasText(props.getDisplayName()) ? props.getDisplayName() : "SSO";
    }
}
