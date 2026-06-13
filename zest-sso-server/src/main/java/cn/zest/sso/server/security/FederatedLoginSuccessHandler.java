package cn.zest.sso.server.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import cn.zest.sso.server.service.IdentitySyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class FederatedLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final IdentitySyncService identitySyncService;
    private final SsoUserDetailsService userDetailsService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauth2User = oauthToken.getPrincipal();
            String username = identitySyncService.provisionOidcUser(oauthToken.getAuthorizedClientRegistrationId(), oauth2User);
            replaceAuthentication(request, username);
        } else if (authentication instanceof Saml2Authentication samlAuth
                && samlAuth.getPrincipal() instanceof Saml2AuthenticatedPrincipal principal) {
            String username = identitySyncService.provisionSamlUser(principal.getRelyingPartyRegistrationId(), principal);
            replaceAuthentication(request, username);
        }
        response.sendRedirect("/");
    }

    private void replaceAuthentication(HttpServletRequest request, String username) {
        SsoUserDetails details = (SsoUserDetails) userDetailsService.loadUserByUsername(username);
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        details, null, details.getAuthorities()));
        request.getSession(true);
    }
}
