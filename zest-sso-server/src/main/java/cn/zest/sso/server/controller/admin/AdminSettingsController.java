package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.server.domain.vo.PasswordPolicyVO;
import cn.zest.sso.server.domain.vo.SettingsVO;
import cn.zest.sso.server.service.PasswordPolicyService;
import cn.zest.sso.server.service.SettingsService;
import cn.zest.sso.server.support.AdminAuditSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SSO_ADMIN')")
public class AdminSettingsController {

    private final SettingsService settingsService;
    private final PasswordPolicyService passwordPolicyService;
    private final AdminAuditSupport auditSupport;

    @GetMapping
    public ApiResponse<SettingsVO> settings() {
        SettingsVO settings = settingsService.getSettings();
        settings.setPasswordPolicy(passwordPolicyService.getPolicy());
        return ApiResponse.success(settings);
    }

    @PutMapping("/password-policy")
    public ApiResponse<PasswordPolicyVO> updatePasswordPolicy(@RequestBody PasswordPolicyVO request) {
        PasswordPolicyVO updated = passwordPolicyService.updatePolicy(request);
        auditSupport.log(AuditEventType.PASSWORD_POLICY_UPDATE,
                auditSupport.currentUser() != null ? auditSupport.currentUser().getUsername() : "system",
                "更新密码策略");
        return ApiResponse.success(updated);
    }
}
