package cn.zest.sso.server.federation.adapter;

import cn.zest.sso.server.domain.dto.CreateIdentityProviderRequest;
import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.federation.spi.FederatedIdpAdapterDescriptor;
import cn.zest.sso.server.federation.support.OAuth2ClientRegistrationSupport;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

@Component
public class GenericOidcFederatedIdpAdapter extends AbstractOAuth2FederatedIdpAdapter {

    public GenericOidcFederatedIdpAdapter(OAuth2ClientRegistrationSupport registrationSupport) {
        super(registrationSupport);
    }

    @Override
    public String adapterKey() {
        return "generic-oidc";
    }

    @Override
    public FederatedIdpAdapterDescriptor descriptor() {
        return new FederatedIdpAdapterDescriptor(
                adapterKey(),
                "通用 OIDC",
                "任意提供 OpenID Discovery 的 IdP（Azure AD、Google、Keycloak 等）",
                true,
                false,
                true,
                Map.of("usernameClaim", "preferred_username", "emailClaim", "email", "displayNameClaim", "name"),
                Map.of()
        );
    }

    @Override
    public void validateCreate(CreateIdentityProviderRequest request, SsoIdentityProvider preview) {
        requireClientCredentials(request);
        if (request.getDiscoveryUri() == null || request.getDiscoveryUri().isBlank()) {
            if (preview == null || !StringUtils.hasText(preview.getDiscoveryUri())) {
                throw new cn.zest.sso.common.exception.SsoException(
                        cn.zest.sso.common.exception.ErrorCode.BAD_REQUEST, "Discovery URI 不能为空");
            }
        }
    }

    @Override
    public void applyDefaults(SsoIdentityProvider provider) {
        if (provider.getScopes() == null || provider.getScopes().isBlank()) {
            provider.setScopes("openid,profile,email");
        }
        if (provider.getUsernameClaim() == null || provider.getUsernameClaim().isBlank()) {
            provider.setUsernameClaim("preferred_username");
        }
    }
}
