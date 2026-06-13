package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.common.api.PageResult;
import cn.zest.sso.server.domain.entity.SsoAccessReviewCampaign;
import cn.zest.sso.server.domain.entity.SsoAccessReviewItem;
import cn.zest.sso.server.security.SsoUserDetails;
import cn.zest.sso.server.service.AccessReviewService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/access-reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SSO_ADMIN', 'SSO_OPERATOR')")
public class AdminAccessReviewController {

    private final AccessReviewService accessReviewService;

    @GetMapping("/campaigns")
    public ApiResponse<PageResult<SsoAccessReviewCampaign>> pageCampaigns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Page<SsoAccessReviewCampaign> result = accessReviewService.pageCampaigns(page, size, status);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @PostMapping("/campaigns")
    @PreAuthorize("hasRole('SSO_ADMIN')")
    public ApiResponse<SsoAccessReviewCampaign> createCampaign(
            @AuthenticationPrincipal SsoUserDetails user,
            @RequestBody CreateCampaignRequest request) {
        return ApiResponse.success(accessReviewService.createCampaign(
                request.name(), request.description(), request.dueAt(), user.getUsername()));
    }

    @PostMapping("/campaigns/{id}/activate")
    @PreAuthorize("hasRole('SSO_ADMIN')")
    public ApiResponse<SsoAccessReviewCampaign> activate(@PathVariable Long id) {
        return ApiResponse.success(accessReviewService.activateCampaign(id));
    }

    @GetMapping("/campaigns/{id}/items")
    public ApiResponse<PageResult<SsoAccessReviewItem>> pageItems(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String decision) {
        Page<SsoAccessReviewItem> result = accessReviewService.pageItems(id, page, size, decision);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/campaigns/{id}/summary")
    public ApiResponse<Map<String, Object>> summary(@PathVariable Long id) {
        return ApiResponse.success(accessReviewService.campaignSummary(id));
    }

    @PostMapping("/items/{id}/decide")
    @PreAuthorize("hasRole('SSO_ADMIN')")
    public ApiResponse<SsoAccessReviewItem> decide(
            @AuthenticationPrincipal SsoUserDetails user,
            @PathVariable Long id,
            @RequestBody DecideRequest request) {
        return ApiResponse.success(accessReviewService.decide(id, request.decision(), user.getUsername()));
    }

    public record CreateCampaignRequest(
            String name,
            String description,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueAt) {}

    public record DecideRequest(String decision) {}
}
