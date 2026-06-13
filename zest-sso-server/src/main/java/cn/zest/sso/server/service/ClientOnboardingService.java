package cn.zest.sso.server.service;

import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.dto.CreateClientRequest;
import cn.zest.sso.server.domain.vo.ClientOnboardingVO;
import cn.zest.sso.server.domain.vo.CreateClientResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientOnboardingService {

    private final SsoProperties ssoProperties;
    private final OAuthClientService clientService;

    public ClientOnboardingVO template(String stack, String redirectUri) {
        String issuer = ssoProperties.getIssuer();
        String effectiveRedirect = StringUtils.hasText(redirectUri) ? redirectUri : "http://localhost:8080/login/oauth2/code/zest-sso";
        Map<String, String> snippets = new LinkedHashMap<>();
        String normalized = stack != null ? stack.toLowerCase() : "spring-boot";
        snippets.put("discovery", issuer + "/.well-known/openid-configuration");
        snippets.put("authorizationEndpoint", issuer + "/oauth2/authorize");
        snippets.put("tokenEndpoint", issuer + "/oauth2/token");
        snippets.put("logoutEndpoint", issuer + "/connect/logout");
        snippets.put("jwksUri", issuer + "/oauth2/jwks");
        snippets.put("redirectUri", effectiveRedirect);
        switch (normalized) {
            case "vue", "spa" -> snippets.put("integration", """
                    // vite.config / .env
                    VITE_SSO_ISSUER=%s
                    VITE_SSO_CLIENT_ID=<your-client-id>
                    VITE_SSO_REDIRECT_URI=%s
                    """.formatted(issuer, effectiveRedirect));
            case "node", "express" -> snippets.put("integration", """
                    process.env.SSO_ISSUER = '%s';
                    process.env.SSO_CLIENT_ID = '<your-client-id>';
                    process.env.SSO_CLIENT_SECRET = '<your-client-secret>';
                    process.env.SSO_REDIRECT_URI = '%s';
                    """.formatted(issuer, effectiveRedirect));
            default -> snippets.put("integration", """
                    zest.sso.client.enabled=true
                    zest.sso.client.issuer=%s
                    zest.sso.client.client-id=<your-client-id>
                    zest.sso.client.client-secret=<your-client-secret>
                    zest.sso.client.redirect-uri=%s
                    """.formatted(issuer, effectiveRedirect));
        }
        return ClientOnboardingVO.builder()
                .stack(normalized)
                .issuer(issuer)
                .recommendedScopes(List.of("openid", "profile", "email", "roles", "tenant"))
                .recommendedGrants(List.of("authorization_code", "refresh_token"))
                .redirectUri(effectiveRedirect)
                .snippets(snippets)
                .build();
    }

    public CreateClientResultVO quickCreate(String appName, String redirectUri) {
        CreateClientRequest request = new CreateClientRequest();
        String slug = appName != null ? appName.toLowerCase().replaceAll("[^a-z0-9]+", "-") : "app";
        if (slug.isBlank()) {
            slug = "app";
        }
        request.setClientId(slug + "-" + UUID.randomUUID().toString().substring(0, 8));
        request.setClientSecret(UUID.randomUUID().toString().replace("-", ""));
        request.setClientName(StringUtils.hasText(appName) ? appName : "Quick App");
        request.setAuthorizationGrantTypes(List.of("authorization_code", "refresh_token"));
        request.setRedirectUris(List.of(StringUtils.hasText(redirectUri)
                ? redirectUri : "http://localhost:8080/login/oauth2/code/" + request.getClientId()));
        request.setScopes(List.of("openid", "profile", "email", "roles", "tenant"));
        request.setRequirePkce(true);
        request.setRequireConsent(false);
        request.setAccessTokenTtl(3600);
        request.setRefreshTokenTtl(86400);
        CreateClientResultVO created = clientService.createClient(request);
        ClientOnboardingVO guide = template("spring-boot", request.getRedirectUris().get(0));
        created.setOnboarding(guide);
        return created;
    }
}
