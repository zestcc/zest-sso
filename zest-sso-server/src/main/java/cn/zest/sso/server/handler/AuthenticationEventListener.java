package cn.zest.sso.server.handler;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.mapper.SsoUserMapper;
import cn.zest.sso.server.security.SsoUserDetails;
import cn.zest.sso.server.metrics.SsoMetrics;
import cn.zest.sso.server.security.LoginAttemptService;
import cn.zest.sso.server.service.AuditService;
import cn.zest.sso.server.service.UserService;
import cn.zest.sso.server.service.WebhookEventPublisher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 认证事件监听器，记录审计日志。
 */
@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final AuditService auditService;
    private final UserService userService;
    private final LoginAttemptService loginAttemptService;
    private final SsoUserMapper userMapper;
    private final SsoMetrics ssoMetrics;
    private final WebhookEventPublisher webhookEventPublisher;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        if (auth.getPrincipal() instanceof SsoUserDetails userDetails) {
            SsoUser user = userMapper.selectById(userDetails.getUserId());
            if (user != null && user.getMfaEnabled() != null && user.getMfaEnabled() == 1) {
                return;
            }
            String ip = resolveClientIp();
            userService.updateLastLogin(userDetails.getUserId(), ip);
            loginAttemptService.onLoginSuccess(userDetails.getUsername());
            ssoMetrics.recordLoginSuccess();
            auditService.log(AuditEventType.LOGIN_SUCCESS, userDetails.getUsername(),
                    null, null, ip, resolveUserAgent(), null);
            webhookEventPublisher.publish(AuditEventType.LOGIN_SUCCESS, userDetails.getUsername(), null, null);
        }
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        loginAttemptService.onLoginFailure(username);
        ssoMetrics.recordLoginFailure();
        auditService.log(AuditEventType.LOGIN_FAILURE, username,
                null, null, resolveClientIp(), resolveUserAgent(),
                event.getException().getMessage());
        webhookEventPublisher.publish(AuditEventType.LOGIN_FAILURE, username, null,
                event.getException().getMessage());
    }

    private String resolveClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return attrs.getRequest().getHeader("User-Agent");
    }
}
