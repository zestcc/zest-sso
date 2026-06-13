package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.common.api.PageResult;
import cn.zest.sso.server.domain.vo.AuthorizationVO;
import cn.zest.sso.server.service.AuthorizationAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/authorizations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SSO_ADMIN')")
public class AdminAuthorizationController {

    private final AuthorizationAdminService authorizationAdminService;

    @GetMapping
    public ApiResponse<PageResult<AuthorizationVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String principalName,
            @RequestParam(required = false) String clientId) {
        List<AuthorizationVO> records = authorizationAdminService.pageAuthorizations(page, size, principalName, clientId);
        long total = authorizationAdminService.countAuthorizations(principalName, clientId);
        return ApiResponse.success(PageResult.of(records, total, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> revoke(@PathVariable String id) {
        authorizationAdminService.revokeAuthorization(id);
        return ApiResponse.success();
    }
}
