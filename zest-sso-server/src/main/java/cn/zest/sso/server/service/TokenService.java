package cn.zest.sso.server.service;

import cn.zest.sso.common.constant.SsoConstants;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.metrics.SsoMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Token 管理服务，支持吊销与黑名单。
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;
    private final SsoProperties ssoProperties;
    private final SsoMetrics ssoMetrics;

    /**
     * 将 Token 加入黑名单。
     *
     * @param token     JWT Token (jti 或完整 token hash)
     * @param ttlSeconds 剩余有效期
     */
    public void revokeToken(String token, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            ttlSeconds = ssoProperties.getToken().getAccessTokenTtl();
        }
        String key = SsoConstants.REDIS_TOKEN_BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "1", ttlSeconds, TimeUnit.SECONDS);
        ssoMetrics.recordTokenRevoked();
    }

    public boolean isTokenRevoked(String token) {
        String key = SsoConstants.REDIS_TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 获取 OIDC Discovery 元数据。
     */
    public Map<String, Object> getOidcDiscoveryMetadata() {
        String issuer = ssoProperties.getIssuer();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("issuer", issuer);
        metadata.put("authorization_endpoint", issuer + "/oauth2/authorize");
        metadata.put("token_endpoint", issuer + "/oauth2/token");
        metadata.put("userinfo_endpoint", issuer + "/userinfo");
        metadata.put("jwks_uri", issuer + "/oauth2/jwks");
        metadata.put("revocation_endpoint", issuer + "/oauth2/revoke");
        metadata.put("introspection_endpoint", issuer + "/oauth2/introspect");
        metadata.put("end_session_endpoint", issuer + "/connect/logout");
        metadata.put("device_authorization_endpoint", issuer + "/oauth2/device_authorization");
        metadata.put("device_authorization_grant_types_supported", new String[]{
                "urn:ietf:params:oauth:grant-type:device_code"
        });
        metadata.put("backchannel_logout_supported", true);
        metadata.put("frontchannel_logout_supported", true);
        metadata.put("response_types_supported", new String[]{"code"});
        metadata.put("grant_types_supported", new String[]{
                "authorization_code", "refresh_token", "client_credentials",
                "urn:ietf:params:oauth:grant-type:device_code"
        });
        metadata.put("subject_types_supported", new String[]{"public"});
        metadata.put("id_token_signing_alg_values_supported", new String[]{"RS256"});
        metadata.put("scopes_supported", new String[]{
                "openid", "profile", "email", "roles", "tenant"
        });
        metadata.put("token_endpoint_auth_methods_supported", new String[]{
                "client_secret_basic", "client_secret_post", "none"
        });
        metadata.put("code_challenge_methods_supported", new String[]{"S256"});
        return metadata;
    }
}
