package cn.zest.sso.server.federation.adapter;

import cn.zest.sso.server.domain.dto.CreateIdentityProviderRequest;
import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.federation.spi.FederatedIdpAdapterDescriptor;
import cn.zest.sso.server.federation.spi.FederatedIdpEndpointConfig;
import cn.zest.sso.server.federation.support.OAuth2ClientRegistrationSupport;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class DingtalkFederatedIdpAdapter extends AbstractOAuth2FederatedIdpAdapter {

    private static final String DEFAULT_DISCOVERY =
            "https://login.dingtalk.com/oauth2/.well-known/openid-configuration";

    public DingtalkFederatedIdpAdapter(OAuth2ClientRegistrationSupport registrationSupport) {
        super(registrationSupport);
    }

    @Override
    public String adapterKey() {
        return "dingtalk";
    }

    @Override
    public FederatedIdpAdapterDescriptor descriptor() {
        return new FederatedIdpAdapterDescriptor(
                adapterKey(),
                "钉钉",
                "钉钉 OAuth2/OIDC；优先 Discovery，可用手动 endpoint_config 覆盖",
                true,
                true,
                true,
                Map.of("usernameClaim", "sub", "emailClaim", "email", "displayNameClaim", "name"),
                Map.of(
                        "discoveryUri", DEFAULT_DISCOVERY,
                        "authorizationUri", "https://login.dingtalk.com/oauth2/auth",
                        "tokenUri", "https://api.dingtalk.com/v1.0/oauth2/userAccessToken"
                )
        );
    }

    @Override
    protected FederatedIdpEndpointConfig defaultEndpoints() {
        FederatedIdpEndpointConfig config = new FederatedIdpEndpointConfig();
        config.setAuthorizationUri("https://login.dingtalk.com/oauth2/auth");
        config.setTokenUri("https://api.dingtalk.com/v1.0/oauth2/userAccessToken");
        return config;
    }

    @Override
    public void validateCreate(CreateIdentityProviderRequest request, SsoIdentityProvider preview) {
        requireClientCredentials(request);
        requireResolvableEndpoints(request, preview);
    }

    @Override
    public void applyDefaults(SsoIdentityProvider provider) {
        if (!StringUtils.hasText(provider.getDiscoveryUri())) {
            provider.setDiscoveryUri(DEFAULT_DISCOVERY);
        }
        if (!StringUtils.hasText(provider.getScopes())) {
            provider.setScopes("openid,profile");
        }
        if (!StringUtils.hasText(provider.getUsernameClaim())) {
            provider.setUsernameClaim("sub");
        }
    }
}
