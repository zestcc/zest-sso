package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.alert.AlertChannelRegistry;
import cn.zest.sso.server.alert.spi.AlertChannelDescriptor;
import cn.zest.sso.server.mfa.MfaChannelRegistry;
import cn.zest.sso.server.mfa.spi.MfaChannelDescriptor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/channels")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SSO_ADMIN', 'SSO_OPERATOR')")
public class AdminChannelController {

    private final MfaChannelRegistry mfaChannelRegistry;
    private final AlertChannelRegistry alertChannelRegistry;

    @GetMapping("/mfa")
    public ApiResponse<List<MfaChannelDescriptor>> listMfaChannels() {
        return ApiResponse.success(mfaChannelRegistry.listDescriptors());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AlertChannelDescriptor>> listAlertChannels() {
        return ApiResponse.success(alertChannelRegistry.listDescriptors());
    }
}
