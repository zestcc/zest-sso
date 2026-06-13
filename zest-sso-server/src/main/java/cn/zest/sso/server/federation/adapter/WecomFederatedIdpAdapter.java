package cn.zest.sso.server.federation.adapter;

import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.dto.CreateIdentityProviderRequest;
import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.federation.spi.FederatedIdpAdapterDescriptor;
import cn.zest.sso.server.federation.spi.FederatedIdpEndpointConfig;
import cn.zest.sso.server.federation.support.OAuth2ClientRegistrationSupport;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 企业微信 OAuth2 联邦 — 换票由 {@link cn.zest.sso.server.federation.oauth.WecomOAuth2TokenClient} 处理。
 */
@Component
public class WecomFederatedIdpAdapter extends AbstractOAuth2FederatedIdpAdapter {

    private final SsoProperties ssoProperties;

    public WecomFederatedIdpAdapter(OAuth2ClientRegistrationSupport registrationSupport,
                                    SsoProperties ssoProperties) {
        super(registrationSupport);
        this.ssoProperties = ssoProperties;
    }

    @Override
    public String adapterKey() {
        return "wecom";
    }

    @Override
    public FederatedIdpAdapterDescriptor descriptor() {
        return new FederatedIdpAdapterDescriptor(
                adapterKey(),
                "企业微信",
                "企业微信 OAuth2 联邦（需 zest.sso.modules.wecom-federation=true）",
                false,
                true,
                ssoProperties.getModules().isWecomFederation(),
                Map.of("usernameClaim", "userid", "emailClaim", "email", "displayNameClaim", "name"),
                Map.of(
                        "authorizationUri", "https://open.weixin.qq.com/connect/oauth2/authorize",
                        "authorizationQueryParams", "response_type=code&scope=snsapi_privateinfo#wechat_redirect"
                )
        );
    }

    @Override
    protected FederatedIdpEndpointConfig defaultEndpoints() {
        FederatedIdpEndpointConfig config = new FederatedIdpEndpointConfig();
        config.setAuthorizationUri("https://open.weixin.qq.com/connect/oauth2/authorize");
        config.setAuthorizationQueryParams("response_type=code&scope=snsapi_privateinfo#wechat_redirect");
        config.setTokenUri("https://qyapi.weixin.qq.com/cgi-bin/gettoken");
        return config;
    }

    @Override
    public void validateCreate(CreateIdentityProviderRequest request, SsoIdentityProvider preview) {
        requireClientCredentials(request);
        requireResolvableEndpoints(request, preview);
    }

    @Override
    public void applyDefaults(SsoIdentityProvider provider) {
        if (!StringUtils.hasText(provider.getScopes())) {
            provider.setScopes("snsapi_privateinfo");
        }
        if (!StringUtils.hasText(provider.getUsernameClaim())) {
            provider.setUsernameClaim("userid");
        }
    }
}
