package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.common.api.PageResult;
import cn.zest.sso.server.domain.dto.CreateLdapProviderRequest;
import cn.zest.sso.server.domain.dto.UpdateLdapProviderRequest;
import cn.zest.sso.server.domain.vo.LdapProviderVO;
import cn.zest.sso.server.service.LdapProviderService;
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
@RequestMapping("/api/admin/ldap-providers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SSO_ADMIN')")
public class AdminLdapController {

    private final LdapProviderService ldapProviderService;

    @GetMapping
    public ApiResponse<PageResult<LdapProviderVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LdapProviderVO> result = ldapProviderService.pageProviders(page, size);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<LdapProviderVO> get(@PathVariable Long id) {
        return ApiResponse.success(ldapProviderService.getById(id));
    }

    @PostMapping
    public ApiResponse<LdapProviderVO> create(@Valid @RequestBody CreateLdapProviderRequest request) {
        return ApiResponse.success(ldapProviderService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<LdapProviderVO> update(@PathVariable Long id, @RequestBody UpdateLdapProviderRequest request) {
        return ApiResponse.success(ldapProviderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ldapProviderService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/test")
    public ApiResponse<String> test(@PathVariable Long id) {
        ldapProviderService.testConnection(id);
        return ApiResponse.success("连接成功");
    }
}
