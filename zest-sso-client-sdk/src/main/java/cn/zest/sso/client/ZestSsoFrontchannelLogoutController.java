package cn.zest.sso.client;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OIDC Front-Channel Logout 页面（iframe 加载用）。
 */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zest.sso.client", name = "frontchannel-logout-enabled", havingValue = "true", matchIfMissing = true)
public class ZestSsoFrontchannelLogoutController {

    @GetMapping(value = "${zest.sso.client.frontchannel-logout-path:/auth/frontchannel-logout}",
            produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> frontchannelLogout(@RequestParam(name = "iss", required = false) String iss) {
        String html = """
                <!DOCTYPE html><html><head><meta charset="UTF-8"><title>Logged out</title></head>
                <body><p>Application session cleared.</p></body></html>
                """;
        return ResponseEntity.ok(html);
    }
}
