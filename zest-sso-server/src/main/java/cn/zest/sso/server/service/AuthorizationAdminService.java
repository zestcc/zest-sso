package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.vo.AuthorizationVO;
import cn.zest.sso.server.support.AdminAuditSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorizationAdminService {

    private final JdbcTemplate jdbcTemplate;
    private final OAuth2AuthorizationService authorizationService;
    private final TokenService tokenService;
    private final AdminAuditSupport auditSupport;
    private final SsoProperties ssoProperties;

    public List<AuthorizationVO> pageAuthorizations(int page, int size, String principalName, String clientId) {
        int offset = Math.max(page - 1, 0) * size;
        StringBuilder sql = new StringBuilder("""
                SELECT id, registered_client_id, principal_name, authorization_grant_type, authorized_scopes,
                       access_token_expires_at, refresh_token_expires_at
                FROM oauth2_authorization
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();
        if (principalName != null && !principalName.isBlank()) {
            sql.append(" AND principal_name LIKE ?");
            params.add("%" + principalName + "%");
        }
        if (clientId != null && !clientId.isBlank()) {
            sql.append(" AND registered_client_id = ?");
            params.add(clientId);
        }
        sql.append(" ORDER BY COALESCE(access_token_issued_at, refresh_token_issued_at) DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            LocalDateTime accessExp = toLocalDateTime(rs.getTimestamp("access_token_expires_at"));
            LocalDateTime refreshExp = toLocalDateTime(rs.getTimestamp("refresh_token_expires_at"));
            boolean active = isActive(accessExp) || isActive(refreshExp);
            return AuthorizationVO.builder()
                    .id(rs.getString("id"))
                    .clientId(rs.getString("registered_client_id"))
                    .principalName(rs.getString("principal_name"))
                    .grantType(rs.getString("authorization_grant_type"))
                    .scopes(rs.getString("authorized_scopes"))
                    .accessTokenExpiresAt(accessExp)
                    .refreshTokenExpiresAt(refreshExp)
                    .active(active)
                    .build();
        });
    }

    public long countAuthorizations(String principalName, String clientId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM oauth2_authorization WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (principalName != null && !principalName.isBlank()) {
            sql.append(" AND principal_name LIKE ?");
            params.add("%" + principalName + "%");
        }
        if (clientId != null && !clientId.isBlank()) {
            sql.append(" AND registered_client_id = ?");
            params.add(clientId);
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public List<String> findDistinctRegisteredClientIdsByPrincipal(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT DISTINCT registered_client_id FROM oauth2_authorization WHERE principal_name = ?",
                (rs, rowNum) -> rs.getString("registered_client_id"),
                principalName);
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeAllByPrincipalName(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            return;
        }
        List<String> ids = jdbcTemplate.query(
                "SELECT id FROM oauth2_authorization WHERE principal_name = ?",
                (rs, rowNum) -> rs.getString("id"),
                principalName);
        for (String id : ids) {
            OAuth2Authorization authorization = authorizationService.findById(id);
            if (authorization == null) {
                continue;
            }
            try {
                revokeAccessToken(authorization);
                authorizationService.remove(authorization);
                auditSupport.log(AuditEventType.TOKEN_REVOKE, authorization.getPrincipalName(),
                        authorization.getRegisteredClientId(), "吊销 OAuth2 授权");
            } catch (SsoException ex) {
                if (ex.getCode() != ErrorCode.NOT_FOUND) {
                    throw ex;
                }
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeAuthorization(String authorizationId) {
        OAuth2Authorization authorization = authorizationService.findById(authorizationId);
        if (authorization == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "授权记录不存在");
        }
        revokeAccessToken(authorization);
        authorizationService.remove(authorization);
        auditSupport.log(AuditEventType.TOKEN_REVOKE, authorization.getPrincipalName(),
                authorization.getRegisteredClientId(), "吊销 OAuth2 授权");
    }

    private void revokeAccessToken(OAuth2Authorization authorization) {
        var accessToken = authorization.getAccessToken();
        if (accessToken == null || accessToken.getToken() == null) {
            return;
        }
        String tokenValue = accessToken.getToken().getTokenValue();
        long ttl = ssoProperties.getToken().getAccessTokenTtl();
        Instant expiresAt = accessToken.getToken().getExpiresAt();
        if (expiresAt != null) {
            ttl = Math.max(Duration.between(Instant.now(), expiresAt).getSeconds(), 1);
        }
        tokenService.revokeToken(hashToken(tokenValue), ttl);
    }

    private boolean isActive(LocalDateTime expiresAt) {
        return expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
