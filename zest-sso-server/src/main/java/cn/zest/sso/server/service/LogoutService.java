package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.server.support.AdminAuditSupport;
import cn.zest.sso.server.metrics.SsoMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 统一登出：会话失效 + OAuth 授权吊销 + Token 黑名单（对标 Keycloak/Okta SLO）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {

    private final AuthorizationAdminService authorizationAdminService;
    private final SessionAdminService sessionAdminService;
    private final BackchannelLogoutService backchannelLogoutService;
    private final AdminAuditSupport auditSupport;
    private final JwtDecoder jwtDecoder;
    private final SsoMetrics ssoMetrics;
    private final WebhookEventPublisher webhookEventPublisher;

    public String resolvePrincipalName(String idTokenHint) {
        String principal = resolvePrincipal(idTokenHint);
        if (!StringUtils.hasText(principal)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                principal = auth.getName();
            }
        }
        return principal;
    }

    public void logoutCurrentUser(String idTokenHint) {
        logoutByPrincipal(resolvePrincipalName(idTokenHint));
    }

    public void logoutByPrincipal(String principal) {
        if (!StringUtils.hasText(principal)) {
            SecurityContextHolder.clearContext();
            return;
        }
        backchannelLogoutService.triggerBackchannelLogout(principal);
        authorizationAdminService.revokeAllByPrincipalName(principal);
        sessionAdminService.revokeAllByUsername(principal);
        auditSupport.log(AuditEventType.LOGOUT, principal, "全局登出：吊销全部 OAuth 授权与会话");
        webhookEventPublisher.publish(AuditEventType.LOGOUT, principal, principal, "全局登出");
        ssoMetrics.recordLogout();
        SecurityContextHolder.clearContext();
    }

    private String resolvePrincipal(String idTokenHint) {
        if (!StringUtils.hasText(idTokenHint)) {
            return null;
        }
        try {
            Jwt jwt = jwtDecoder.decode(idTokenHint);
            String username = jwt.getClaimAsString("preferred_username");
            return StringUtils.hasText(username) ? username : jwt.getSubject();
        } catch (Exception ex) {
            log.debug("无法解析 id_token_hint: {}", ex.getMessage());
            return null;
        }
    }
}
