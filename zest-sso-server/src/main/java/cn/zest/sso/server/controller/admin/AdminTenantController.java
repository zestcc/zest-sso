package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.common.api.PageResult;
import cn.zest.sso.server.domain.dto.CreateTenantRequest;
import cn.zest.sso.server.domain.dto.UpdateTenantRequest;
import cn.zest.sso.server.domain.vo.TenantVO;
import cn.zest.sso.server.service.TenantService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SSO_ADMIN')")
public class AdminTenantController {

    private final TenantService tenantService;

    @GetMapping
    public ApiResponse<PageResult<TenantVO>> listTenants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TenantVO> result = tenantService.pageTenants(page, size);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantVO> getTenant(@PathVariable Long id) {
        return ApiResponse.success(tenantService.getById(id));
    }

    @PostMapping
    public ApiResponse<TenantVO> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return ApiResponse.success(tenantService.createTenant(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TenantVO> updateTenant(@PathVariable Long id,
                                              @RequestBody UpdateTenantRequest request) {
        return ApiResponse.success(tenantService.updateTenant(id, request));
    }

    @PostMapping("/{id}/enable")
    public ApiResponse<Void> enableTenant(@PathVariable Long id) {
        tenantService.enableTenant(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/disable")
    public ApiResponse<Void> disableTenant(@PathVariable Long id) {
        tenantService.disableTenant(id);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTenant(@PathVariable Long id) {
        tenantService.deleteTenant(id);
        return ApiResponse.success();
    }
}
