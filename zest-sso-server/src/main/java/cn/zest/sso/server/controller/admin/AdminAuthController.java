package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.dto.ChangePasswordRequest;
import cn.zest.sso.server.domain.dto.LoginRequest;
import cn.zest.sso.server.domain.dto.MfaEnableRequest;
import cn.zest.sso.server.domain.dto.MfaVerifyRequest;
import cn.zest.sso.server.domain.vo.LoginResultVO;
import cn.zest.sso.server.domain.vo.MfaSetupVO;
import cn.zest.sso.server.domain.vo.UserInfoVO;
import cn.zest.sso.server.security.SsoUserDetails;
import cn.zest.sso.server.security.SsoUserDetailsService;
import cn.zest.sso.server.service.LogoutService;
import cn.zest.sso.server.service.MfaService;
import cn.zest.sso.server.service.UserService;
import cn.zest.sso.server.service.WebLoginService;
import cn.zest.sso.server.support.AdminAuditSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final MfaService mfaService;
    private final SsoUserDetailsService userDetailsService;
    private final AdminAuditSupport auditSupport;
    private final WebLoginService webLoginService;
    private final LogoutService logoutService;

    @PostMapping("/login")
    public ApiResponse<LoginResultVO> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest,
                                            HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            SsoUserDetails details = (SsoUserDetails) authentication.getPrincipal();
            if (!userService.hasAdminRole(details.getUserId())) {
                throw new SsoException(ErrorCode.FORBIDDEN, "该账号无管理台访问权限");
            }
            LoginResultVO result = mfaService.buildLoginResult(details.getUserId(), resolveClientIp(httpRequest));
            if (!result.isMfaRequired()) {
                webLoginService.establishSession(authentication, httpRequest, httpResponse);
            }
            return ApiResponse.success(result);
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("用户名或密码错误");
        }
    }

    @PostMapping("/mfa/verify")
    public ApiResponse<UserInfoVO> verifyMfa(@Valid @RequestBody MfaVerifyRequest request,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        UserInfoVO user = mfaService.verifyLogin(request.getMfaToken(), request.getCode());
        SsoUserDetails details = userDetailsService.loadByUserId(user.getId());
        Authentication authentication = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        webLoginService.establishSession(authentication, httpRequest, httpResponse);
        return ApiResponse.success(user);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        SsoUserDetails user = auditSupport.currentUser();
        if (user != null) {
            logoutService.revokePrincipalAccess(user.getUsername());
        }
        logoutService.finishHttpLogout(request, response);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoVO> currentUser(@AuthenticationPrincipal SsoUserDetails userDetails) {
        return ApiResponse.success(userService.getUserInfo(userDetails.getUserId()));
    }

    @GetMapping("/mfa/setup")
    public ApiResponse<MfaSetupVO> mfaSetup(@AuthenticationPrincipal SsoUserDetails userDetails) {
        return ApiResponse.success(mfaService.getSetupInfo(userDetails.getUserId()));
    }

    @PostMapping("/mfa/enable")
    public ApiResponse<Void> enableMfa(@AuthenticationPrincipal SsoUserDetails userDetails,
                                         @Valid @RequestBody MfaEnableRequest request) {
        mfaService.enable(userDetails.getUserId(), request.getCode());
        return ApiResponse.success();
    }

    @PostMapping("/mfa/disable")
    public ApiResponse<Void> disableMfa(@AuthenticationPrincipal SsoUserDetails userDetails,
                                          @Valid @RequestBody MfaEnableRequest request) {
        mfaService.disable(userDetails.getUserId(), request.getCode());
        return ApiResponse.success();
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal SsoUserDetails userDetails,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUserId(), request.getCurrentPassword(), request.getNewPassword());
        return ApiResponse.success();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
