package cn.zest.sso.server.security;

import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.domain.mapper.SsoIdentityProviderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseClientRegistrationRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final SsoIdentityProviderMapper identityProviderMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        SsoIdentityProvider provider = identityProviderMapper.selectOne(new LambdaQueryWrapper<SsoIdentityProvider>()
                .eq(SsoIdentityProvider::getAlias, registrationId)
                .eq(SsoIdentityProvider::getProviderType, "OIDC")
                .eq(SsoIdentityProvider::getEnabled, 1));
        if (provider == null) {
            return null;
        }
        return toClientRegistration(provider);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        List<ClientRegistration> registrations = new ArrayList<>();
        identityProviderMapper.selectList(new LambdaQueryWrapper<SsoIdentityProvider>()
                        .eq(SsoIdentityProvider::getProviderType, "OIDC")
                        .eq(SsoIdentityProvider::getEnabled, 1))
                .forEach(provider -> registrations.add(toClientRegistration(provider)));
        return registrations.iterator();
    }

    private ClientRegistration toClientRegistration(SsoIdentityProvider provider) {
        JsonNode discovery = fetchDiscovery(provider.getDiscoveryUri());
        String[] scopes = provider.getScopes().split(",");
        for (int i = 0; i < scopes.length; i++) {
            scopes[i] = scopes[i].trim();
        }
        return ClientRegistration.withRegistrationId(provider.getAlias())
                .clientId(provider.getClientId())
                .clientSecret(provider.getClientSecret())
                .authorizationUri(text(discovery, "authorization_endpoint"))
                .tokenUri(text(discovery, "token_endpoint"))
                .jwkSetUri(text(discovery, "jwks_uri"))
                .userInfoUri(text(discovery, "userinfo_endpoint"))
                .scope(scopes)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .userNameAttributeName(resolveUsernameClaim(provider))
                .build();
    }

    private String resolveUsernameClaim(SsoIdentityProvider provider) {
        if (provider.getUsernameClaim() != null && !provider.getUsernameClaim().isBlank()) {
            return provider.getUsernameClaim();
        }
        return "sub";
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
}
