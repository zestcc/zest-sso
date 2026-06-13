package cn.zest.sso.server.service;

import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.metrics.SsoMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SsoProperties ssoProperties;

    @Mock
    private SsoMetrics ssoMetrics;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void shouldReturnOidcDiscoveryMetadata() {
        when(ssoProperties.getIssuer()).thenReturn("http://localhost:9000");

        Map<String, Object> metadata = tokenService.getOidcDiscoveryMetadata();

        assertEquals("http://localhost:9000", metadata.get("issuer"));
        assertNotNull(metadata.get("authorization_endpoint"));
        assertNotNull(metadata.get("jwks_uri"));
    }

    @Test
    void shouldRevokeToken() {
        SsoProperties.Token token = new SsoProperties.Token();
        token.setAccessTokenTtl(3600);
        lenient().when(ssoProperties.getToken()).thenReturn(token);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenService.revokeToken("test-jti", 1800);

        verify(valueOperations).set(anyString(), eq("1"), eq(1800L), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldCheckTokenRevoked() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        assertTrue(tokenService.isTokenRevoked("revoked-token"));
    }

    @Test
    void shouldCheckTokenNotRevoked() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        assertFalse(tokenService.isTokenRevoked("valid-token"));
    }
}
