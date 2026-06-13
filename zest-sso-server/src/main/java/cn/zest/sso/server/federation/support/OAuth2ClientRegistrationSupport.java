package cn.zest.sso.server.federation.support;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.federation.spi.FederatedIdpEndpointConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

@Component
@RequiredArgsConstructor
public class OAuth2ClientRegistrationSupport {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public ClientRegistration build(SsoIdentityProvider provider,
                                    FederatedIdpEndpointConfig manualEndpoints) {
        String[] scopes = parseScopes(provider.getScopes());
        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(provider.getAlias())
                .clientId(provider.getClientId())
                .clientSecret(provider.getClientSecret())
                .scope(scopes)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .userNameAttributeName(resolveUsernameClaim(provider));

        if (hasManualEndpoints(manualEndpoints)) {
            applyManual(builder, manualEndpoints);
        } else if (StringUtils.hasText(provider.getDiscoveryUri())) {
            applyDiscovery(builder, provider.getDiscoveryUri());
        } else {
            throw new SsoException(ErrorCode.BAD_REQUEST, "需要 Discovery URI 或 endpoint_config 手动端点");
        }
        return builder.build();
    }

    public FederatedIdpEndpointConfig parseEndpointConfig(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, FederatedIdpEndpointConfig.class);
        } catch (Exception e) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "endpoint_config JSON 无效");
        }
    }

    private void applyDiscovery(ClientRegistration.Builder builder, String discoveryUri) {
        JsonNode discovery = fetchDiscovery(discoveryUri);
        builder.authorizationUri(text(discovery, "authorization_endpoint"));
        builder.tokenUri(text(discovery, "token_endpoint"));
        if (discovery.hasNonNull("userinfo_endpoint")) {
            builder.userInfoUri(discovery.get("userinfo_endpoint").asText());
        }
        if (discovery.hasNonNull("jwks_uri")) {
            builder.jwkSetUri(discovery.get("jwks_uri").asText());
        }
    }

    private void applyManual(ClientRegistration.Builder builder, FederatedIdpEndpointConfig config) {
        if (!StringUtils.hasText(config.getAuthorizationUri()) || !StringUtils.hasText(config.getTokenUri())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "手动端点缺少 authorizationUri 或 tokenUri");
        }
        String authUri = config.getAuthorizationUri();
        if (StringUtils.hasText(config.getAuthorizationQueryParams())) {
            authUri = authUri + (authUri.contains("?") ? "&" : "?") + config.getAuthorizationQueryParams();
        }
        builder.authorizationUri(authUri);
        builder.tokenUri(config.getTokenUri());
        if (StringUtils.hasText(config.getUserInfoUri())) {
            builder.userInfoUri(config.getUserInfoUri());
        }
        if (StringUtils.hasText(config.getJwkSetUri())) {
            builder.jwkSetUri(config.getJwkSetUri());
        }
    }

    private boolean hasManualEndpoints(FederatedIdpEndpointConfig config) {
        return config != null
                && StringUtils.hasText(config.getAuthorizationUri())
                && StringUtils.hasText(config.getTokenUri());
    }

    private JsonNode fetchDiscovery(String discoveryUri) {
        try {
            String body = restTemplate.getForObject(discoveryUri, String.class);
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("无法加载 OIDC Discovery: " + discoveryUri, e);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalStateException("Discovery 缺少字段: " + field);
        }
        return value.asText();
    }

    private String[] parseScopes(String scopes) {
        if (!StringUtils.hasText(scopes)) {
            return new String[] {"openid", "profile"};
        }
        return java.util.Arrays.stream(scopes.split(",")).map(String::trim).filter(StringUtils::hasText).toArray(String[]::new);
    }

    private String resolveUsernameClaim(SsoIdentityProvider provider) {
        if (provider.getUsernameClaim() != null && !provider.getUsernameClaim().isBlank()) {
            return provider.getUsernameClaim();
        }
        return "sub";
    }
}
