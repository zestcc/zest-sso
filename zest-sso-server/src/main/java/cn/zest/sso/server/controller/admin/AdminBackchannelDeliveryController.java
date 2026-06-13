package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.common.api.PageResult;
import cn.zest.sso.server.domain.vo.BackchannelDeliveryVO;
import cn.zest.sso.server.service.BackchannelLogoutDeliveryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/logout-deliveries")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SSO_ADMIN', 'SSO_OPERATOR')")
public class AdminBackchannelDeliveryController {

    private final BackchannelLogoutDeliveryService deliveryService;

    @GetMapping
    public ApiResponse<PageResult<BackchannelDeliveryVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String principalName) {
        Page<BackchannelDeliveryVO> result = deliveryService.page(page, size, status, principalName);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('SSO_ADMIN')")
    public ApiResponse<Void> retry(@PathVariable Long id) {
        deliveryService.deliver(deliveryService.findByIdOrThrow(id));
        return ApiResponse.success();
    }
}
