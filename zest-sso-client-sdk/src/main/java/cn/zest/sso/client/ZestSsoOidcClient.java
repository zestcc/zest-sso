package cn.zest.sso.client;

import cn.zest.sso.common.constant.SsoConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ZestSSO OIDC 客户端工具，供接入应用使用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZestSsoOidcClient {

    private final ZestSsoClientProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, Object> discoveryCache = new ConcurrentHashMap<>();
    private volatile long discoveryCacheTime;

    /**
     * 构建 Authorization Code + PKCE 授权 URL。
     */
    public AuthorizationRequest buildAuthorizationUrl() {
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        String state = generateState();

        String authEndpoint = resolveAuthorizationEndpoint();
        String scopes = String.join(" ", properties.getScopes());

        String url = authEndpoint
                + "?response_type=code"
                + "&client_id=" + encode(properties.getClientId())
                + "&redirect_uri=" + encode(properties.getRedirectUri())
                + "&scope=" + encode(scopes)
                + "&state=" + encode(state)
                + "&code_challenge=" + encode(codeChallenge)
                + "&code_challenge_method=S256";

        return AuthorizationRequest.builder()
                .authorizationUrl(url)
                .state(state)
                .codeVerifier(codeVerifier)
                .build();
    }

    /**
     * 从 JWT Claims Map 解析用户身份。
     */
    @SuppressWarnings("unchecked")
    public SsoUserPrincipal parsePrincipal(Map<String, Object> claims) {
        Object userId = claims.get(SsoConstants.CLAIM_USER_ID);
        Object roles = claims.get(SsoConstants.CLAIM_ROLES);
        Object tenantId = claims.get(SsoConstants.CLAIM_TENANT_ID);

        return SsoUserPrincipal.builder()
                .subject(String.valueOf(claims.get("sub")))
                .userId(userId != null ? Long.valueOf(userId.toString()) : null)
                .username(getStringClaim(claims, properties.getUsernameClaim()))
                .email(getStringClaim(claims, SsoConstants.CLAIM_EMAIL))
                .displayName(getStringClaim(claims, SsoConstants.CLAIM_NAME))
                .roles(roles instanceof List ? (List<String>) roles : List.of())
                .tenantId(tenantId != null ? Long.valueOf(tenantId.toString()) : null)
                .build();
    }

    /**
     * 构建 RP-Initiated Logout URL（对标 Okta end_session_endpoint）。
     */
    public String buildLogoutUrl(String postLogoutRedirectUri, String idTokenHint) {
        String endSession = resolveEndSessionEndpoint();
        StringBuilder url = new StringBuilder(endSession)
                .append("?post_logout_redirect_uri=").append(encode(postLogoutRedirectUri));
        if (idTokenHint != null && !idTokenHint.isBlank()) {
            url.append("&id_token_hint=").append(encode(idTokenHint));
        }
        return url.toString();
    }

    /**
     * 强制刷新 OIDC Discovery 缓存。
     */
    public void refreshDiscoveryCache() {
        discoveryCache.clear();
        discoveryCacheTime = 0;
        resolveAuthorizationEndpoint();
    }

    private String resolveEndSessionEndpoint() {
        ensureDiscoveryLoaded();
        Object endpoint = discoveryCache.get("end_session_endpoint");
        if (endpoint instanceof String s && !s.isBlank()) {
            return s;
        }
        return properties.getIssuer().replaceAll("/$", "") + "/connect/logout";
    }

    @SuppressWarnings("unchecked")
    private void ensureDiscoveryLoaded() {
        if (System.currentTimeMillis() - discoveryCacheTime < discoveryTtlMs()
                && discoveryCache.containsKey("authorization_endpoint")) {
            return;
        }
        String discoveryUrl = properties.getIssuer() + "/.well-known/openid-configuration";
        try {
            Map<String, Object> metadata = restTemplate.getForObject(discoveryUrl, Map.class);
            if (metadata != null) {
                discoveryCache.clear();
                discoveryCache.putAll(metadata);
                discoveryCacheTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            log.warn("OIDC Discovery 获取失败: {}", e.getMessage());
        }
    }

    private long discoveryTtlMs() {
        return Math.max(properties.getJwksCacheSeconds(), 60) * 1000L;
    }

    @SuppressWarnings("unchecked")
    private String resolveAuthorizationEndpoint() {
        ensureDiscoveryLoaded();
        Object endpoint = discoveryCache.get("authorization_endpoint");
        if (endpoint instanceof String s && !s.isBlank()) {
            return s;
        }
        return properties.getIssuer() + "/oauth2/authorize";
    }

    private String getStringClaim(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        return value != null ? value.toString() : null;
    }

    private String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String generateState() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
