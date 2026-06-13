package cn.zest.sso.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OIDC Back-Channel Logout 接收端点（供 ZestFlow / ZestLLM 等 RP 挂载）。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnBean(SsoLogoutHandler.class)
@ConditionalOnProperty(prefix = "zest.sso.client", name = "backchannel-logout-enabled", havingValue = "true", matchIfMissing = true)
public class ZestSsoBackchannelLogoutController {

    private final ZestSsoClientProperties properties;
    private final ZestSsoLogoutTokenValidator logoutTokenValidator;
    private final SsoLogoutHandler logoutHandler;

    @PostMapping(value = "${zest.sso.client.backchannel-logout-path:/auth/backchannel-logout}",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> backchannelLogout(@RequestParam(name = "logout_token", required = false) String logoutToken) {
        if (!StringUtils.hasText(logoutToken)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            String principal = logoutTokenValidator.validateAndExtractSubject(logoutToken);
            logoutHandler.onBackchannelLogout(principal);
            log.info("Back-Channel Logout 已处理: principal={}, client={}", principal, properties.getClientId());
            return ResponseEntity.ok().build();
        } catch (JwtException ex) {
            log.warn("Back-Channel Logout token 无效: {}", ex.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
