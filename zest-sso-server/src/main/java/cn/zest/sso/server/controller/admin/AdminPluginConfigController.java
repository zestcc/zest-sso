package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.domain.vo.PluginConfigVO;
import cn.zest.sso.server.plugin.PluginCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/plugins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SSO_ADMIN')")
public class AdminPluginConfigController {

    private final PluginCatalogService pluginCatalogService;

    @GetMapping
    public ApiResponse<List<PluginConfigVO>> list() {
        return ApiResponse.success(pluginCatalogService.listPluginConfigs());
    }

    @PutMapping("/{pluginKey}")
    public ApiResponse<PluginConfigVO> save(@PathVariable String pluginKey,
                                              @RequestBody SavePluginConfigRequest request) {
        return ApiResponse.success(pluginCatalogService.savePluginConfig(
                pluginKey, request.enabled(), request.config()));
    }

    public record SavePluginConfigRequest(boolean enabled, Map<String, String> config) {
    }
}
