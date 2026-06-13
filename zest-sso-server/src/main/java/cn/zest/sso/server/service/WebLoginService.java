package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.server.security.LoginAttemptService;
import cn.zest.sso.server.security.SsoUserDetails;
import cn.zest.sso.server.security.SsoUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebLoginService {

    private final MfaService mfaService;
    private final SsoUserDetailsService userDetailsService;
    private final AuditService auditService;
    private final UserService userService;
    private final LoginAttemptService loginAttemptService;

    public String completeMfaLogin(String mfaToken, String code, HttpServletRequest request) {
        MfaService.WebLoginResult result = mfaService.verifyWebLogin(mfaToken, code);
        SsoUserDetails details = userDetailsService.loadByUserId(result.userId());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                details, null, details.getAuthorities());
        establishSession(authentication, request, null);

        String ip = resolveClientIp(request);
        userService.updateLastLogin(result.userId(), ip);
        loginAttemptService.onLoginSuccess(details.getUsername());
        auditService.log(AuditEventType.LOGIN_SUCCESS, details.getUsername(),
                null, null, ip, request.getHeader("User-Agent"), null);

        return result.redirectUrl();
    }

    public void establishSession(Authentication authentication, HttpServletRequest request,
                                 jakarta.servlet.http.HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    }

    public void establishWebAuthnSession(Authentication authentication, HttpServletRequest request,
                                         jakarta.servlet.http.HttpServletResponse response) {
        establishSession(authentication, request, response);
        SsoUserDetails details = (SsoUserDetails) authentication.getPrincipal();
        String ip = resolveClientIp(request);
        userService.updateLastLogin(details.getUserId(), ip);
        loginAttemptService.onLoginSuccess(details.getUsername());
        auditService.log(AuditEventType.LOGIN_SUCCESS, details.getUsername(),
                null, null, ip, request.getHeader("User-Agent"), "Passkey 登录");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
