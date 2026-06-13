package cn.zest.sso.server.federation.adapter;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.dto.CreateIdentityProviderRequest;
import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.federation.spi.FederatedIdpAdapter;
import cn.zest.sso.server.federation.spi.FederatedIdpEndpointConfig;
import cn.zest.sso.server.federation.support.OAuth2ClientRegistrationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public abstract class AbstractOAuth2FederatedIdpAdapter implements FederatedIdpAdapter {

    protected final OAuth2ClientRegistrationSupport registrationSupport;

    @Override
    public boolean supports(SsoIdentityProvider provider) {
        return provider != null && "OIDC".equalsIgnoreCase(provider.getProviderType());
    }

    @Override
    public ClientRegistration buildClientRegistration(SsoIdentityProvider provider) {
        FederatedIdpEndpointConfig manual = registrationSupport.parseEndpointConfig(provider.getEndpointConfig());
        return registrationSupport.build(provider, mergeEndpoints(manual));
    }

    protected FederatedIdpEndpointConfig mergeEndpoints(FederatedIdpEndpointConfig configured) {
        FederatedIdpEndpointConfig defaults = defaultEndpoints();
        if (defaults == null) {
            return configured;
        }
        if (configured == null) {
            return defaults;
        }
        FederatedIdpEndpointConfig merged = new FederatedIdpEndpointConfig();
        merged.setAuthorizationUri(firstNonBlank(configured.getAuthorizationUri(), defaults.getAuthorizationUri()));
        merged.setTokenUri(firstNonBlank(configured.getTokenUri(), defaults.getTokenUri()));
        merged.setUserInfoUri(firstNonBlank(configured.getUserInfoUri(), defaults.getUserInfoUri()));
        merged.setJwkSetUri(firstNonBlank(configured.getJwkSetUri(), defaults.getJwkSetUri()));
        merged.setAuthorizationQueryParams(
                firstNonBlank(configured.getAuthorizationQueryParams(), defaults.getAuthorizationQueryParams()));
        return merged;
    }

    protected FederatedIdpEndpointConfig defaultEndpoints() {
        return null;
    }

    protected void requireClientCredentials(CreateIdentityProviderRequest request) {
        if (!StringUtils.hasText(request.getClientId())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "Client ID 不能为空");
        }
        if (!StringUtils.hasText(request.getClientSecret())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "Client Secret 不能为空");
        }
    }

    protected void requireDiscoveryOrManual(CreateIdentityProviderRequest request) {
        requireResolvableEndpoints(request, null);
    }

    protected void requireResolvableEndpoints(CreateIdentityProviderRequest request, SsoIdentityProvider preview) {
        if (hasDiscovery(request, preview) || hasManualEndpoints(request, preview) || hasAdapterDefaultEndpoints()) {
            return;
        }
        throw new SsoException(ErrorCode.BAD_REQUEST, "需要 Discovery URI 或 endpoint_config 手动端点");
    }

    private boolean hasDiscovery(CreateIdentityProviderRequest request, SsoIdentityProvider preview) {
        return StringUtils.hasText(request.getDiscoveryUri())
                || (preview != null && StringUtils.hasText(preview.getDiscoveryUri()));
    }

    private boolean hasManualEndpoints(CreateIdentityProviderRequest request, SsoIdentityProvider preview) {
        if (request.getEndpointConfig() != null
                && StringUtils.hasText(request.getEndpointConfig().getAuthorizationUri())
                && StringUtils.hasText(request.getEndpointConfig().getTokenUri())) {
            return true;
        }
        if (preview == null || !StringUtils.hasText(preview.getEndpointConfig())) {
            return false;
        }
        FederatedIdpEndpointConfig parsed = registrationSupport.parseEndpointConfig(preview.getEndpointConfig());
        return parsed != null
                && StringUtils.hasText(parsed.getAuthorizationUri())
                && StringUtils.hasText(parsed.getTokenUri());
    }

    private boolean hasAdapterDefaultEndpoints() {
        FederatedIdpEndpointConfig defaults = defaultEndpoints();
        return defaults != null
                && StringUtils.hasText(defaults.getAuthorizationUri())
                && StringUtils.hasText(defaults.getTokenUri());
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }
}
