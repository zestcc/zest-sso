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
public class FeishuFederatedIdpAdapter extends AbstractOAuth2FederatedIdpAdapter {

    private static final String DEFAULT_DISCOVERY = "https://open.feishu.cn/.well-known/openid-configuration";

    public FeishuFederatedIdpAdapter(OAuth2ClientRegistrationSupport registrationSupport) {
        super(registrationSupport);
    }

    @Override
    public String adapterKey() {
        return "feishu";
    }

    @Override
    public FederatedIdpAdapterDescriptor descriptor() {
        return new FederatedIdpAdapterDescriptor(
                adapterKey(),
                "飞书",
                "飞书开放平台 OIDC 联邦登录（推荐国内 SMB）",
                true,
                true,
                true,
                Map.of("usernameClaim", "sub", "emailClaim", "email", "displayNameClaim", "name"),
                Map.of("discoveryUri", DEFAULT_DISCOVERY)
        );
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
            provider.setScopes("openid,profile,email");
        }
        if (!StringUtils.hasText(provider.getUsernameClaim())) {
            provider.setUsernameClaim("sub");
        }
        if (!StringUtils.hasText(provider.getDisplayNameClaim())) {
            provider.setDisplayNameClaim("name");
        }
    }
}
