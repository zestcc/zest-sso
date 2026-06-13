package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.domain.vo.AlertChannelConfigVO;
import cn.zest.sso.server.domain.vo.PluginConfigVO;
import cn.zest.sso.server.plugin.AlertChannelConfigService;
import cn.zest.sso.server.plugin.PluginCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/alert-channels")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SSO_ADMIN')")
public class AdminAlertChannelController {

    private final AlertChannelConfigService alertChannelConfigService;

    @GetMapping
    public ApiResponse<List<AlertChannelConfigVO>> list() {
        return ApiResponse.success(alertChannelConfigService.listAll());
    }

    @PostMapping
    public ApiResponse<AlertChannelConfigVO> create(@RequestBody AlertChannelConfigVO request) {
        return ApiResponse.success(alertChannelConfigService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AlertChannelConfigVO> update(@PathVariable Long id, @RequestBody AlertChannelConfigVO request) {
        return ApiResponse.success(alertChannelConfigService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        alertChannelConfigService.delete(id);
        return ApiResponse.success();
    }
}
