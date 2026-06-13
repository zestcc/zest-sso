package cn.zest.sso.server.controller;

import cn.zest.sso.common.constant.SsoConstants;
import cn.zest.sso.server.domain.vo.UserInfoVO;
import cn.zest.sso.server.security.SsoUserDetails;
import cn.zest.sso.server.service.TokenService;
import cn.zest.sso.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * OIDC UserInfo 端点。
 */
@RestController
@RequiredArgsConstructor
public class OidcUserInfoController {

    private final UserService userService;

    @GetMapping("/userinfo")
    public Map<String, Object> userInfo(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getClaimAsString(SsoConstants.CLAIM_USERNAME);
        if (username == null) {
            username = jwt.getSubject();
        }
        UserInfoVO userInfo = userService.getUserInfoByUsername(username);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", String.valueOf(userInfo.getId()));
        claims.put(SsoConstants.CLAIM_USERNAME, userInfo.getUsername());
        claims.put(SsoConstants.CLAIM_EMAIL, userInfo.getEmail());
        claims.put(SsoConstants.CLAIM_NAME, userInfo.getDisplayName());
        claims.put(SsoConstants.CLAIM_ROLES, userInfo.getRoles());
        if (userInfo.getDefaultTenantId() != null) {
            claims.put(SsoConstants.CLAIM_TENANT_ID, userInfo.getDefaultTenantId());
        }
        return claims;
    }
}
