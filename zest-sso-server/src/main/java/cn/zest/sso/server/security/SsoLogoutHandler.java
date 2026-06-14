package cn.zest.sso.server.security;

import cn.zest.sso.server.service.LogoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Spring Security /logout 联动 OAuth 授权吊销、Back-Channel 与 Redis 会话清理。
 */
@Component
@RequiredArgsConstructor
public class SsoLogoutHandler implements LogoutHandler {

    private final LogoutService logoutService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        String principal = authentication.getName();
        if (StringUtils.hasText(principal)) {
            logoutService.revokePrincipalAccess(principal);
        }
    }
}
