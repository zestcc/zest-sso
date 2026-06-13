package cn.zest.sso.server.security;

import cn.zest.sso.server.service.TokenService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * JWT 解码器装饰：校验 Redis 黑名单（吊销 Token 立即失效）。
 */
public class RevokedTokenJwtDecoder implements JwtDecoder {

    private final JwtDecoder delegate;
    private final TokenService tokenService;

    public RevokedTokenJwtDecoder(JwtDecoder delegate, TokenService tokenService) {
        this.delegate = delegate;
        this.tokenService = tokenService;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = delegate.decode(token);
        if (tokenService.isTokenRevoked(hashToken(token))) {
            throw new JwtException("Token has been revoked");
        }
        String jti = jwt.getId();
        if (jti != null && tokenService.isTokenRevoked(jti)) {
            throw new JwtException("Token has been revoked");
        }
        return jwt;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
