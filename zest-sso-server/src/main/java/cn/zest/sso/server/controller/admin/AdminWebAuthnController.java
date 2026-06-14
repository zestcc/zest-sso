package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.domain.dto.WebauthnFinishRequest;
import cn.zest.sso.server.domain.dto.WebauthnRegisterOptionsRequest;
import cn.zest.sso.server.domain.vo.WebauthnCredentialVO;
import cn.zest.sso.server.domain.vo.WebauthnOptionsVO;
import cn.zest.sso.server.security.SsoUserDetails;
import cn.zest.sso.server.service.WebAuthnService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/auth/webauthn")
@RequiredArgsConstructor
public class AdminWebAuthnController {

    private final WebAuthnService webAuthnService;

    @GetMapping("/credentials")
    public ApiResponse<List<WebauthnCredentialVO>> list(@AuthenticationPrincipal SsoUserDetails user) {
        return ApiResponse.success(webAuthnService.listCredentials(user.getUserId()));
    }

    @PostMapping("/register/options")
    public ApiResponse<WebauthnOptionsVO> registerOptions(
            @AuthenticationPrincipal SsoUserDetails user,
            @RequestBody(required = false) WebauthnRegisterOptionsRequest request,
            HttpServletRequest httpRequest) {
        String nickname = request != null && request.getNickname() != null ? request.getNickname() : "Passkey";
        String origin = webAuthnService.resolveWebOrigin(
                httpRequest.getHeader("Origin"), httpRequest.getHeader("Referer"));
        return ApiResponse.success(webAuthnService.beginRegistration(
                user.getUserId(), nickname, origin));
    }

    @PostMapping("/register/finish")
    public ApiResponse<Void> registerFinish(
            @AuthenticationPrincipal SsoUserDetails user,
            @RequestBody WebauthnFinishRequest request,
            HttpServletRequest httpRequest) {
        webAuthnService.finishRegistration(
                user.getUserId(),
                request.getSessionToken(),
                request.getNickname() != null ? request.getNickname() : "Passkey",
                request.getCredential(),
                webAuthnService.resolveWebOrigin(httpRequest.getHeader("Origin"), httpRequest.getHeader("Referer")));
        return ApiResponse.success();
    }

    @DeleteMapping("/credentials/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal SsoUserDetails user,
                                    @PathVariable Long id) {
        webAuthnService.deleteCredential(user.getUserId(), id);
        return ApiResponse.success();
    }
}
