package cn.zest.sso.client.rp.oidc;

import cn.zest.sso.client.rp.RpSsoException;
import cn.zest.sso.client.rp.RpSsoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OIDC id_token JWKS 校验。
 */
@Slf4j
@RequiredArgsConstructor
public class OidcJwtValidator {

    private static final Duration JWKS_CACHE_TTL = Duration.ofHours(1);

    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;
    private final Map<String, CachedJwks> jwksCache = new ConcurrentHashMap<>();

    public Claims parseAndValidate(String jwt, OidcEndpoints endpoints, RpSsoProperties props) {
        if (!StringUtils.hasText(jwt)) {
            throw new RpSsoException("INVALID_TOKEN", "Token 为空");
        }
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            throw new RpSsoException("INVALID_TOKEN", "Token 格式错误");
        }
        String kid = readKid(parts[0]);
        PublicKey publicKey = resolvePublicKey(endpoints.jwksUri(), kid);
        Jws<Claims> jws;
        try {
            jws = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(jwt);
        } catch (ExpiredJwtException ex) {
            throw new RpSsoException("TOKEN_EXPIRED", "Token 已过期");
        } catch (Exception ex) {
            throw new RpSsoException("INVALID_TOKEN", "Token 无效");
        }
        Claims claims = jws.getPayload();
        if (claims.getExpiration() != null && claims.getExpiration().toInstant().isBefore(Instant.now())) {
            throw new RpSsoException("TOKEN_EXPIRED", "Token 已过期");
        }
        String expectedIssuer = endpoints.issuer();
        if (StringUtils.hasText(expectedIssuer) && !expectedIssuer.equals(claims.getIssuer())) {
            throw new RpSsoException("INVALID_TOKEN", "Issuer 不匹配");
        }
        validateAudience(claims, props.getClientId());
        return claims;
    }

    private void validateAudience(Claims claims, String clientId) {
        Object aud = claims.get("aud");
        if (aud instanceof String audStr && !clientId.equals(audStr)) {
            throw new RpSsoException("INVALID_TOKEN", "Audience 不匹配");
        }
        if (aud instanceof java.util.List<?> audList && !audList.contains(clientId)) {
            throw new RpSsoException("INVALID_TOKEN", "Audience 不匹配");
        }
    }

    private String readKid(String headerPart) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(headerPart);
            JsonNode header = objectMapper.readTree(decoded);
            return header.path("kid").asText(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private PublicKey resolvePublicKey(String jwksUri, String kid) {
        CachedJwks cached = jwksCache.get(jwksUri);
        if (cached == null || cached.expiresAt().isBefore(Instant.now())) {
            cached = loadJwks(jwksUri);
            jwksCache.put(jwksUri, cached);
        }
        JsonNode keyNode = findKey(cached.keys(), kid);
        if (keyNode == null) {
            cached = loadJwks(jwksUri);
            jwksCache.put(jwksUri, cached);
            keyNode = findKey(cached.keys(), kid);
        }
        if (keyNode == null) {
            throw new RpSsoException("INVALID_TOKEN", "JWKS 密钥未找到");
        }
        return toPublicKey(keyNode);
    }

    private CachedJwks loadJwks(String jwksUri) {
        try {
            String body = restClientBuilder.build()
                    .get()
                    .uri(jwksUri)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            return new CachedJwks(root.path("keys"), Instant.now().plus(JWKS_CACHE_TTL));
        } catch (Exception ex) {
            log.warn("JWKS 加载失败: {}", jwksUri, ex);
            throw new RpSsoException("INVALID_TOKEN", "JWKS 加载失败", ex);
        }
    }

    private JsonNode findKey(JsonNode keys, String kid) {
        if (keys == null || !keys.isArray()) {
            return null;
        }
        for (JsonNode key : keys) {
            if (!StringUtils.hasText(kid) || kid.equals(key.path("kid").asText(null))) {
                if ("RSA".equals(key.path("kty").asText())) {
                    return key;
                }
            }
        }
        return keys.size() > 0 ? keys.get(0) : null;
    }

    private PublicKey toPublicKey(JsonNode keyNode) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(keyNode.path("n").asText());
            byte[] eBytes = Base64.getUrlDecoder().decode(keyNode.path("e").asText());
            RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, nBytes), new BigInteger(1, eBytes));
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            throw new RpSsoException("INVALID_TOKEN", "JWKS 公钥解析失败", ex);
        }
    }

    private record CachedJwks(JsonNode keys, Instant expiresAt) {
    }
}
