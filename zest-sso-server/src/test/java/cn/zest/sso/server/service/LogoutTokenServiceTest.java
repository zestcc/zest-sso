package cn.zest.sso.server.service;

import cn.zest.sso.server.config.SsoProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private SsoProperties ssoProperties;

    @Mock
    private JwtSigningKeyService jwtSigningKeyService;

    @InjectMocks
    private LogoutTokenService logoutTokenService;

    @Test
    void shouldCreateLogoutToken() {
        when(ssoProperties.getIssuer()).thenReturn("http://localhost:9000");
        when(jwtSigningKeyService.activeKeyId()).thenReturn("test-key");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(
                Jwt.withTokenValue("logout-token-jwt")
                        .header("alg", "RS256")
                        .claim("sub", "admin")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(120))
                        .build());

        String token = logoutTokenService.createLogoutToken("admin", "zestflow");

        assertNotNull(token);
        assertEquals("logout-token-jwt", token);
    }
}
