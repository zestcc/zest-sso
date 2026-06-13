package cn.zest.sso.server.security;

import cn.zest.sso.server.domain.entity.SsoOAuthClient;
import cn.zest.sso.server.domain.mapper.SsoOAuthClientMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于数据库的 OAuth2 客户端注册仓库。
 */
@Component
@Primary
@RequiredArgsConstructor
public class DatabaseRegisteredClientRepository implements RegisteredClientRepository {

    private final SsoOAuthClientMapper clientMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void save(RegisteredClient registeredClient) {
        // 客户端通过 Admin API 管理，此处不实现动态保存
        throw new UnsupportedOperationException("请通过 Admin API 管理 OAuth 客户端");
    }

    @Override
    public RegisteredClient findById(String id) {
        SsoOAuthClient client = clientMapper.selectById(Long.parseLong(id));
        return client != null ? toRegisteredClient(client) : null;
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        SsoOAuthClient client = clientMapper.selectOne(new LambdaQueryWrapper<SsoOAuthClient>()
                .eq(SsoOAuthClient::getClientId, clientId)
                .eq(SsoOAuthClient::getStatus, 1));
        return client != null ? toRegisteredClient(client) : null;
    }

    public RegisteredClient toRegisteredClient(SsoOAuthClient client) {
        Set<String> redirectUris = parseCommaSeparated(client.getRedirectUris());
        Set<String> scopes = parseCommaSeparated(client.getScopes());
        Set<String> configuredAuthMethods = parseCommaSeparated(client.getClientAuthenticationMethods());
        boolean publicClient = configuredAuthMethods.size() == 1
                && configuredAuthMethods.contains(ClientAuthenticationMethod.NONE.getValue());

        RegisteredClient.Builder builder = RegisteredClient.withId(String.valueOf(client.getId()))
                .clientId(client.getClientId())
                .clientName(client.getClientName());

        if (!publicClient && StringUtils.hasText(client.getClientSecretHash())) {
            builder.clientSecret(client.getClientSecretHash());
        }

        if (publicClient) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        } else {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
            configuredAuthMethods.forEach(method ->
                    builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method)));
        }
        if (StringUtils.hasText(client.getMtlsCertificateThumbprints())) {
            builder.clientAuthenticationMethod(new ClientAuthenticationMethod("tls_client_auth"));
        }

        parseCommaSeparated(client.getAuthorizationGrantTypes()).forEach(grantType ->
                builder.authorizationGrantType(new AuthorizationGrantType(grantType)));

        redirectUris.forEach(builder::redirectUri);
        scopes.forEach(builder::scope);

        if (!scopes.contains("openid")) {
            builder.scope("openid");
        }

        builder.clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(client.getRequireConsent() != null && client.getRequireConsent() == 1)
                .requireProofKey(client.getRequirePkce() != null && client.getRequirePkce() == 1)
                .build());

        builder.tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofSeconds(
                        client.getAccessTokenTtl() != null ? client.getAccessTokenTtl() : 3600))
                .refreshTokenTimeToLive(Duration.ofSeconds(
                        client.getRefreshTokenTtl() != null ? client.getRefreshTokenTtl() : 86400))
                .reuseRefreshTokens(false)
                .build());

        return builder.build();
    }

    private Set<String> parseCommaSeparated(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }
}
