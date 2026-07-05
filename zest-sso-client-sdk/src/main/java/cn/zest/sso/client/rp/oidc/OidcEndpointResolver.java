package cn.zest.sso.client.rp.oidc;

import cn.zest.sso.client.rp.RpSsoException;
import cn.zest.sso.client.rp.RpSsoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OIDC Discovery 或静态配置解析端点。
 */
@Slf4j
@RequiredArgsConstructor
public class OidcEndpointResolver {

    private static final Duration DISCOVERY_CACHE_TTL = Duration.ofHours(1);

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedDiscovery> discoveryCache = new ConcurrentHashMap<>();

    public OidcEndpoints resolve(RpSsoProperties props) {
        if (StringUtils.hasText(props.getDiscoveryUri())) {
            return resolveFromDiscovery(props.getDiscoveryUri());
        }
        return resolveFromStatic(props);
    }

    private OidcEndpoints resolveFromDiscovery(String discoveryUri) {
        CachedDiscovery cached = discoveryCache.get(discoveryUri);
        if (cached == null || cached.expiresAt().isBefore(Instant.now())) {
            cached = loadDiscovery(discoveryUri);
            discoveryCache.put(discoveryUri, cached);
        }
        return cached.endpoints();
    }

    private CachedDiscovery loadDiscovery(String discoveryUri) {
        try {
            String body = restClientBuilder.build()
                    .get()
                    .uri(discoveryUri)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(body);
            String issuer = root.path("issuer").asText(null);
            if (!StringUtils.hasText(issuer)) {
                throw new RpSsoException("DISCOVERY_ERROR", "OIDC Discovery 缺少 issuer");
            }
            OidcEndpoints endpoints = new OidcEndpoints(
                    issuer,
                    requiredText(root, "authorization_endpoint"),
                    requiredText(root, "token_endpoint"),
                    requiredText(root, "jwks_uri"),
                    root.path("end_session_endpoint").asText(null)
            );
            return new CachedDiscovery(endpoints, Instant.now().plus(DISCOVERY_CACHE_TTL));
        } catch (RpSsoException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("OIDC Discovery 加载失败: {}", discoveryUri, ex);
            throw new RpSsoException("DISCOVERY_ERROR", "OIDC Discovery 加载失败", ex);
        }
    }

    private OidcEndpoints resolveFromStatic(RpSsoProperties props) {
        String issuer = props.getIssuer().replaceAll("/$", "");
        String jwksUri = StringUtils.hasText(props.getJwksUri()) ? props.getJwksUri() : issuer + "/oauth2/jwks";
        return new OidcEndpoints(
                issuer,
                issuer + "/oauth2/authorize",
                issuer + "/oauth2/token",
                jwksUri,
                issuer + "/connect/logout"
        );
    }

    private static String requiredText(JsonNode root, String field) {
        String value = root.path(field).asText(null);
        if (!StringUtils.hasText(value)) {
            throw new RpSsoException("DISCOVERY_ERROR", "OIDC Discovery 缺少 " + field);
        }
        return value;
    }

    private record CachedDiscovery(OidcEndpoints endpoints, Instant expiresAt) {
    }
}
