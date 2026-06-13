package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.domain.vo.SessionVO;
import cn.zest.sso.server.service.SessionAdminService;
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
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SSO_ADMIN')")
public class AdminSessionController {

    private final SessionAdminService sessionAdminService;

    @GetMapping
    public ApiResponse<List<SessionVO>> list(@RequestParam(required = false) String username) {
        return ApiResponse.success(sessionAdminService.listSessions(username));
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> revoke(@PathVariable String sessionId) {
        sessionAdminService.revokeSession(sessionId);
        return ApiResponse.success();
    }
}
