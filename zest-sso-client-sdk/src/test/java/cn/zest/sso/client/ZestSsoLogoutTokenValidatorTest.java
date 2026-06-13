package cn.zest.sso.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZestSsoLogoutTokenValidatorTest {

    @Mock
    private ZestSsoClientProperties properties;

    @Mock
    private JwtDecoder jwtDecoder;

    @InjectMocks
    private ZestSsoLogoutTokenValidator validator;

    @Test
    void shouldValidateLogoutToken() {
        when(properties.getClientId()).thenReturn("zest-llm-admin");
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("admin")
                .claim("preferred_username", "admin")
                .claim("events", Map.of("http://schemas.openid.net/event/backchannel-logout", Map.of()))
                .audience(List.of("zest-llm-admin"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(120))
                .build();
        when(jwtDecoder.decode("token")).thenReturn(jwt);

        assertEquals("admin", validator.validateAndExtractSubject("token"));
    }

    @Test
    void shouldRejectMissingEvent() {
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("admin")
                .audience(List.of("zest-llm-admin"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(120))
                .build();
        when(jwtDecoder.decode("token")).thenReturn(jwt);

        assertThrows(Exception.class, () -> validator.validateAndExtractSubject("token"));
    }
}
