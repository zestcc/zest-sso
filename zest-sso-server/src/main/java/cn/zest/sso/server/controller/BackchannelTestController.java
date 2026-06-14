package cn.zest.sso.server.controller;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.service.BackchannelTestSeedService;
import cn.zest.sso.server.support.BackchannelTestReceiver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Back-Channel 联调专用：内置测试 RP 与授权种子（生产环境默认关闭）。
 */
@RestController
@RequestMapping("/api/public/test")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zest.sso.test", name = "backchannel-receiver-enabled", havingValue = "true")
public class BackchannelTestController {

    private final BackchannelTestReceiver testReceiver;
    private final BackchannelTestSeedService seedService;
    private final JwtDecoder jwtDecoder;

    @PostMapping("/rp/backchannel-logout")
    public ApiResponse<Void> receiveLogoutToken(@RequestParam("logout_token") String logoutToken) {
        Jwt jwt = jwtDecoder.decode(logoutToken);
        String principal = jwt.getClaimAsString("preferred_username");
        if (principal == null || principal.isBlank()) {
            principal = jwt.getSubject();
        }
        String audience = jwt.getAudience() != null && !jwt.getAudience().isEmpty()
                ? jwt.getAudience().get(0) : "unknown";
        testReceiver.record(principal, audience, logoutToken);
        return ApiResponse.success();
    }

    @GetMapping("/rp/backchannel-logout/last")
    public ApiResponse<Map<String, Object>> lastReceipt() {
        BackchannelTestReceiver.Receipt receipt = testReceiver.lastReceipt();
        if (receipt == null) {
            return ApiResponse.success(Map.of("received", false));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("received", true);
        body.put("principalName", receipt.principalName());
        body.put("audienceClientId", receipt.clientId());
        body.put("receivedAt", receipt.receivedAt().toString());
        return ApiResponse.success(body);
    }

    @PostMapping("/backchannel/seed")
    public ApiResponse<Void> seedAuthorization(
            @RequestParam(defaultValue = "backchannel-test-rp") String clientId) {
        seedService.seedAdminAuthorization(clientId);
        return ApiResponse.success();
    }

    @PostMapping("/backchannel/clear")
    public ApiResponse<Void> clearReceipt() {
        testReceiver.clear();
        return ApiResponse.success();
    }
}
