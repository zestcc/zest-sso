package cn.zest.sso.server.controller;

import cn.zest.sso.common.api.ApiResponse;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.service.BackchannelLogoutService;
import cn.zest.sso.server.service.LogoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * OIDC RP-Initiated Logout（单点登出），联动 Back/Front-Channel Logout。
 */
@Controller
@RequiredArgsConstructor
public class OidcLogoutController {

    private final SsoProperties ssoProperties;
    private final LogoutService logoutService;
    private final BackchannelLogoutService backchannelLogoutService;

    @GetMapping({"/connect/logout", "/logout/oidc"})
    public void oidcLogout(
            @RequestParam(name = "post_logout_redirect_uri", required = false) String redirectUri,
            @RequestParam(name = "id_token_hint", required = false) String idTokenHint,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String principal = logoutService.resolvePrincipalName(idTokenHint);
        List<String> frontchannelUris = StringUtils.hasText(principal)
                ? backchannelLogoutService.resolveFrontchannelLogoutUris(principal)
                : List.of();

        if (StringUtils.hasText(principal)) {
            logoutService.revokeOAuthAccess(principal);
        }
        logoutService.finishHttpLogout(request, response);

        String finalRedirect = StringUtils.hasText(redirectUri) && isAllowedRedirect(redirectUri)
                ? redirectUri
                : "/login?logout";

        if (!frontchannelUris.isEmpty()) {
            writeFrontchannelLogoutPage(response, frontchannelUris, finalRedirect);
            return;
        }
        response.sendRedirect(finalRedirect);
    }

    @GetMapping("/api/public/logout-url")
    @ResponseBody
    public ApiResponse<String> buildLogoutUrl(
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "id_token_hint", required = false) String idTokenHint) {
        if (!isAllowedRedirect(redirectUri)) {
            return ApiResponse.fail(400, "redirect_uri 不在白名单");
        }
        StringBuilder url = new StringBuilder(ssoProperties.getIssuer().replaceAll("/$", ""))
                .append("/connect/logout?post_logout_redirect_uri=")
                .append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
        if (StringUtils.hasText(idTokenHint)) {
            url.append("&id_token_hint=").append(URLEncoder.encode(idTokenHint, StandardCharsets.UTF_8));
        }
        return ApiResponse.success(url.toString());
    }

    private void writeFrontchannelLogoutPage(HttpServletResponse response,
                                             List<String> frontchannelUris,
                                             String redirectUri) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        out.println("<meta http-equiv=\"refresh\" content=\"2;url=" + escapeHtml(redirectUri) + "\">");
        out.println("<title>正在登出…</title></head><body><p>正在通知各应用登出，请稍候…</p>");
        for (String uri : frontchannelUris) {
            out.println("<iframe src=\"" + escapeHtml(uri) + "\" style=\"display:none\" width=\"0\" height=\"0\"></iframe>");
        }
        out.println("</body></html>");
        out.flush();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
    }

    private boolean isAllowedRedirect(String redirectUri) {
        try {
            URI uri = URI.create(redirectUri);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            for (String allowed : ssoProperties.getSecurity().getLogoutRedirectHosts()) {
                if (host.equals(allowed) || host.endsWith("." + allowed)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}
