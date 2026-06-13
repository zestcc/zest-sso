package cn.zest.sso.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 验证 ZestSSO 下发的 OIDC Back-Channel Logout Token。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZestSsoLogoutTokenValidator {

    private static final String BACKCHANNEL_LOGOUT_EVENT = "http://schemas.openid.net/event/backchannel-logout";

    private final ZestSsoClientProperties properties;
    private final JwtDecoder zestSsoJwtDecoder;

    /**
     * 校验 logout_token，返回主体标识（preferred_username 或 sub）。
     */
    public String validateAndExtractSubject(String logoutToken) {
        if (logoutToken == null || logoutToken.isBlank()) {
            throw new JwtException("logout_token 不能为空");
        }
        Jwt jwt = zestSsoJwtDecoder.decode(logoutToken);
        assertAudience(jwt);
        assertBackchannelLogoutEvent(jwt);
        String username = jwt.getClaimAsString("preferred_username");
        return username != null && !username.isBlank() ? username : jwt.getSubject();
    }

    @SuppressWarnings("unchecked")
    private void assertBackchannelLogoutEvent(Jwt jwt) {
        Object events = jwt.getClaim("events");
        if (!(events instanceof Map<?, ?> eventMap) || !eventMap.containsKey(BACKCHANNEL_LOGOUT_EVENT)) {
            throw new JwtException("logout_token 缺少 backchannel-logout 事件");
        }
    }

    private void assertAudience(Jwt jwt) {
        String expected = properties.getClientId();
        if (expected == null || expected.isBlank()) {
            return;
        }
        List<String> audience = jwt.getAudience();
        if (audience == null || !audience.contains(expected)) {
            throw new JwtException("logout_token aud 不匹配当前客户端");
        }
    }
}
