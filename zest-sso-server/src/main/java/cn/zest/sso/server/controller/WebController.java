package cn.zest.sso.server.controller;

import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.vo.UserInfoVO;
import cn.zest.sso.server.security.SsoUserDetails;
import cn.zest.sso.server.service.IdentityProviderService;
import cn.zest.sso.server.service.UserService;
import cn.zest.sso.server.service.WebLoginService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Web 页面控制器。
 */
@Controller
@RequiredArgsConstructor
public class WebController {

    private final UserService userService;
    private final IdentityProviderService identityProviderService;
    private final WebLoginService webLoginService;
    private final SsoProperties ssoProperties;

    @GetMapping("/login")
    public String loginPage(Model model,
                            @RequestParam(value = "step", required = false) String step,
                            @RequestParam(value = "token", required = false) String token,
                            @RequestParam(value = "mfaError", required = false) String mfaError,
                            @RequestParam(value = "lang", required = false) String lang) {
        SsoProperties.Branding branding = ssoProperties.getBranding();
        model.addAttribute("identityProviders", identityProviderService.listEnabledPublic());
        model.addAttribute("mfaStep", "mfa".equals(step));
        model.addAttribute("mfaToken", token);
        model.addAttribute("mfaError", mfaError != null);
        model.addAttribute("loginTitle", branding.getLoginTitle());
        model.addAttribute("loginSubtitle", branding.getLoginSubtitle());
        model.addAttribute("logoUrl", branding.getLogoUrl());
        model.addAttribute("primaryColor", branding.getPrimaryColor());
        model.addAttribute("locale", lang != null ? lang : branding.getDefaultLocale());
        return "login";
    }

    @PostMapping("/login/mfa")
    public String verifyMfa(@RequestParam("token") String token,
                            @RequestParam("code") String code,
                            HttpServletRequest request) {
        try {
            String redirect = webLoginService.completeMfaLogin(token, code, request);
            return "redirect:" + redirect;
        } catch (Exception e) {
            return "redirect:/login?step=mfa&token=" + token + "&mfaError=1";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal SsoUserDetails userDetails, Model model) {
        if (userDetails != null) {
            UserInfoVO userInfo = userService.getUserInfo(userDetails.getUserId());
            model.addAttribute("user", userInfo);
        }
        return "index";
    }
}
