package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.common.api.PageResult;
import cn.zest.sso.server.domain.dto.CreateIdentityProviderRequest;
import cn.zest.sso.server.domain.dto.ParseSamlMetadataRequest;
import cn.zest.sso.server.domain.dto.UpdateIdentityProviderRequest;
import cn.zest.sso.server.domain.vo.IdentityProviderVO;
import cn.zest.sso.server.domain.vo.SamlMetadataVO;
import cn.zest.sso.server.federation.spi.FederatedIdpAdapterDescriptor;
import cn.zest.sso.server.service.IdentityProviderService;
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
@RequestMapping("/api/admin/identity-providers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SSO_ADMIN')")
public class AdminIdentityProviderController {

    private final IdentityProviderService identityProviderService;

    @GetMapping("/adapters")
    public ApiResponse<java.util.List<FederatedIdpAdapterDescriptor>> listAdapters() {
        return ApiResponse.success(identityProviderService.listAdapters());
    }

    @GetMapping
    public ApiResponse<PageResult<IdentityProviderVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<IdentityProviderVO> result = identityProviderService.pageProviders(page, size);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<IdentityProviderVO> get(@PathVariable Long id) {
        return ApiResponse.success(identityProviderService.getById(id));
    }

    @PostMapping("/parse-saml-metadata")
    public ApiResponse<SamlMetadataVO> parseSamlMetadata(@Valid @RequestBody ParseSamlMetadataRequest request) {
        return ApiResponse.success(identityProviderService.parseSamlMetadata(request.getMetadataUri()));
    }

    @PostMapping
    public ApiResponse<IdentityProviderVO> create(@Valid @RequestBody CreateIdentityProviderRequest request) {
        return ApiResponse.success(identityProviderService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<IdentityProviderVO> update(@PathVariable Long id,
                                                    @RequestBody UpdateIdentityProviderRequest request) {
        return ApiResponse.success(identityProviderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        identityProviderService.delete(id);
        return ApiResponse.success();
    }
}
