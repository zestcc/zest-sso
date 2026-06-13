package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.common.api.PageResult;
import cn.zest.sso.server.domain.dto.CreateGroupRequest;
import cn.zest.sso.server.domain.dto.UpdateGroupRequest;
import cn.zest.sso.server.domain.vo.GroupVO;
import cn.zest.sso.server.service.GroupService;
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

import java.util.List;

@RestController
@RequestMapping("/api/admin/groups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SSO_ADMIN')")
public class AdminGroupController {

    private final GroupService groupService;

    @GetMapping
    public ApiResponse<PageResult<GroupVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Page<GroupVO> result = groupService.pageGroups(page, size, keyword);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/all")
    public ApiResponse<List<GroupVO>> listAll() {
        return ApiResponse.success(groupService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<GroupVO> get(@PathVariable Long id) {
        return ApiResponse.success(groupService.getById(id));
    }

    @PostMapping
    public ApiResponse<GroupVO> create(@Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.success(groupService.createGroup(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<GroupVO> update(@PathVariable Long id, @RequestBody UpdateGroupRequest request) {
        return ApiResponse.success(groupService.updateGroup(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ApiResponse.success();
    }
}
