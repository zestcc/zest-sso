package cn.zest.sso.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class OAuth2ConsentController {

    private final RegisteredClientRepository registeredClientRepository;

    @GetMapping("/oauth2/consent")
    public String consent(Principal principal, Model model,
                          @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
                          @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
                          @RequestParam(OAuth2ParameterNames.STATE) String state) {
        RegisteredClient client = registeredClientRepository.findByClientId(clientId);
        model.addAttribute("clientId", clientId);
        model.addAttribute("clientName", client != null ? client.getClientName() : clientId);
        model.addAttribute("state", state);
        model.addAttribute("principalName", principal.getName());
        model.addAttribute("scopes", parseScopes(scope));
        model.addAttribute("scopeParam", scope);
        return "consent";
    }

    private Set<String> parseScopes(String scope) {
        Set<String> scopes = new HashSet<>();
        if (StringUtils.hasText(scope)) {
            scopes.addAll(Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
        }
        return scopes;
    }
}
