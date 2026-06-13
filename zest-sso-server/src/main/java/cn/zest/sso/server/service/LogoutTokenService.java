package cn.zest.sso.server.service;

import cn.zest.sso.server.config.SsoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 签发 OIDC Back-Channel Logout Token（OpenID Connect Back-Channel Logout 1.0）。
 */
@Service
@RequiredArgsConstructor
public class LogoutTokenService {

    private static final String BACKCHANNEL_LOGOUT_EVENT = "http://schemas.openid.net/event/backchannel-logout";

    private final JwtEncoder jwtEncoder;
    private final SsoProperties ssoProperties;
    private final JwtSigningKeyService jwtSigningKeyService;

    public String createLogoutToken(String subject, String audienceClientId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ssoProperties.getIssuer())
                .subject(subject)
                .audience(List.of(audienceClientId))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(120))
                .id(UUID.randomUUID().toString())
                .claim("events", Map.of(BACKCHANNEL_LOGOUT_EVENT, Map.of()))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(jwtSigningKeyService.activeKeyId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
