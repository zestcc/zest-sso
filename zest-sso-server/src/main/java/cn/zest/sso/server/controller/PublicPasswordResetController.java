package cn.zest.sso.server.controller;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.domain.dto.ForgotPasswordRequest;
import cn.zest.sso.server.domain.dto.ResetPasswordByTokenRequest;
import cn.zest.sso.server.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/password")
@RequiredArgsConstructor
public class PublicPasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot")
    public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getUsernameOrEmail());
        return ApiResponse.success();
    }

    @PostMapping("/reset")
    public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordByTokenRequest request) {
        passwordResetService.completeReset(request.getToken(), request.getNewPassword());
        return ApiResponse.success();
    }
}
