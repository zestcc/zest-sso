package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.modules.OptionalModuleRegistry;
import cn.zest.sso.server.modules.spi.OptionalModuleDescriptor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/modules")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SSO_ADMIN', 'SSO_OPERATOR')")
public class AdminOptionalModuleController {

    private final OptionalModuleRegistry moduleRegistry;

    @GetMapping
    public ApiResponse<List<OptionalModuleDescriptor>> list() {
        return ApiResponse.success(moduleRegistry.listDescriptors());
    }
}
