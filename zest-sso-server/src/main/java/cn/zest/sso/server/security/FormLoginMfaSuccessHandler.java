package cn.zest.sso.server.security;

import cn.zest.sso.server.domain.vo.LoginResultVO;
import cn.zest.sso.server.service.MfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class FormLoginMfaSuccessHandler implements AuthenticationSuccessHandler {

    private final MfaService mfaService;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SsoUserDetails details = (SsoUserDetails) authentication.getPrincipal();
        String clientIp = resolveClientIp(request);
        LoginResultVO result = mfaService.buildLoginResult(details.getUserId(), clientIp);

        if (result.isMfaRequired()) {
            SavedRequest saved = requestCache.getRequest(request, response);
            String redirectUrl = saved != null ? saved.getRedirectUrl() : "/";
            requestCache.removeRequest(request, response);
            mfaService.attachRedirectUrl(result.getMfaToken(), redirectUrl);
            clearAuthentication(request);
            response.sendRedirect(request.getContextPath() + "/login?step=mfa&token=" + result.getMfaToken());
            return;
        }

        response.sendRedirect(resolveTargetUrl(request, response));
    }

    private void clearAuthentication(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        }
    }

    private String resolveTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            requestCache.removeRequest(request, response);
            return saved.getRedirectUrl();
        }
        return request.getContextPath() + "/";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
