package cn.zest.sso.server.support;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.server.security.SsoUserDetails;
import cn.zest.sso.server.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 管理操作审计辅助，自动填充操作者与请求上下文。
 */
@Component
@RequiredArgsConstructor
public class AdminAuditSupport {

    private final AuditService auditService;

    public void log(AuditEventType eventType, String target, String detail) {
        auditService.log(eventType, currentActor(), target, null, clientIp(), userAgent(), detail);
    }

    public void log(AuditEventType eventType, String target, String clientId, String detail) {
        auditService.log(eventType, currentActor(), target, clientId, clientIp(), userAgent(), detail);
    }

    public String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SsoUserDetails details) {
            return details.getUsername();
        }
        return "system";
    }

    public SsoUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SsoUserDetails details) {
            return details;
        }
        return null;
    }

    public boolean isSsoAdmin() {
        SsoUserDetails user = currentUser();
        return user != null && user.getRoles().contains("SSO_ADMIN");
    }

    public boolean isTenantAdminOnly() {
        SsoUserDetails user = currentUser();
        if (user == null) {
            return false;
        }
        return user.getRoles().contains("TENANT_ADMIN")
                && !user.getRoles().contains("SSO_ADMIN")
                && !user.getRoles().contains("SSO_OPERATOR");
    }

    public Long currentTenantId() {
        SsoUserDetails user = currentUser();
        return user != null ? user.getDefaultTenantId() : null;
    }

    private String clientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getHeader("User-Agent") : null;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
