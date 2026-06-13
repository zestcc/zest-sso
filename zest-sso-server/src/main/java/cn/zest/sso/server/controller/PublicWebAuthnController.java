package cn.zest.sso.server.controller;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.domain.dto.WebauthnFinishRequest;
import cn.zest.sso.server.domain.dto.WebauthnLoginOptionsRequest;
import cn.zest.sso.server.domain.vo.WebauthnOptionsVO;
import cn.zest.sso.server.security.SsoUserDetails;
import cn.zest.sso.server.security.SsoUserDetailsService;
import cn.zest.sso.server.service.WebAuthnService;
import cn.zest.sso.server.service.WebLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/webauthn")
@RequiredArgsConstructor
public class PublicWebAuthnController {

    private final WebAuthnService webAuthnService;
    private final WebLoginService webLoginService;
    private final SsoUserDetailsService userDetailsService;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @PostMapping("/login/options")
    public ApiResponse<WebauthnOptionsVO> loginOptions(@RequestBody(required = false) WebauthnLoginOptionsRequest request,
                                                       HttpServletRequest httpRequest) {
        String username = request != null ? request.getUsername() : null;
        return ApiResponse.success(webAuthnService.beginAuthentication(username, httpRequest.getHeader("Origin")));
    }

    @PostMapping("/login/finish")
    public ApiResponse<Map<String, String>> loginFinish(@Valid @RequestBody WebauthnFinishRequest request,
                                                        HttpServletRequest httpRequest,
                                                        HttpServletResponse httpResponse) {
        Long userId = webAuthnService.finishAuthentication(
                request.getSessionToken(), request.getCredential(), httpRequest.getHeader("Origin"));
        SsoUserDetails details = userDetailsService.loadByUserId(userId);
        var authentication = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        webLoginService.establishWebAuthnSession(authentication, httpRequest, httpResponse);
        return ApiResponse.success(Map.of("redirectUrl", resolveRedirectUrl(httpRequest, httpResponse)));
    }

    private String resolveRedirectUrl(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved != null) {
            requestCache.removeRequest(request, response);
            return saved.getRedirectUrl();
        }
        return request.getContextPath() + "/";
    }
}
