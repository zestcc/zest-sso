package cn.zest.sso.server.controller.admin;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.domain.vo.JwtSigningKeyVO;
import cn.zest.sso.server.service.JwtSigningKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/jwt-keys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SSO_ADMIN')")
public class AdminJwtKeyController {

    private final JwtSigningKeyService jwtSigningKeyService;

    @GetMapping
    public ApiResponse<List<JwtSigningKeyVO>> list() {
        return ApiResponse.success(jwtSigningKeyService.listKeys());
    }

    @PostMapping("/rotate")
    public ApiResponse<JwtSigningKeyVO> rotate() throws Exception {
        var key = jwtSigningKeyService.rotate();
        return ApiResponse.success(JwtSigningKeyVO.builder()
                .id(key.getId())
                .keyId(key.getKeyId())
                .status(key.getStatus())
                .notAfter(key.getNotAfter())
                .createTime(key.getCreateTime())
                .build());
    }
}
