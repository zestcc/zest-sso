package cn.zest.sso.server.controller;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.domain.vo.IdentityProviderVO;
import cn.zest.sso.server.service.IdentityProviderService;
import cn.zest.sso.server.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 公开 API 端点。
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicApiController {

    private final TokenService tokenService;
    private final IdentityProviderService identityProviderService;

    @GetMapping("/.well-known/openid-configuration")
    public Map<String, Object> openidConfiguration() {
        return tokenService.getOidcDiscoveryMetadata();
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("UP");
    }

    @GetMapping("/identity-providers")
    public ApiResponse<List<IdentityProviderVO>> identityProviders() {
        return ApiResponse.success(identityProviderService.listEnabledPublic());
    }
}
