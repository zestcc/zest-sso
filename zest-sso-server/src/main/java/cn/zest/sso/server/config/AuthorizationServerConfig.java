package cn.zest.sso.server.config;

import cn.zest.sso.server.security.DevicePublicClientAuthenticationConverter;
import cn.zest.sso.server.security.DevicePublicClientAuthenticationProvider;
import cn.zest.sso.server.security.JwtKeyManager;
import cn.zest.sso.server.security.RevokedTokenJwtDecoder;
import cn.zest.sso.server.service.TokenService;
import cn.zest.sso.server.service.JwtSigningKeyService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2/OIDC 授权服务器安全配置。
 */
@Configuration
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    private final SsoProperties ssoProperties;
    private final JwtKeyManager jwtKeyManager;
    private final JwtSigningKeyService jwtSigningKeyService;
    private final RegisteredClientRepository registeredClientRepository;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                http.getConfigurer(OAuth2AuthorizationServerConfigurer.class);
        // RP-Initiated Logout 由 OidcLogoutController 处理，避免 SAS 强制 id_token_hint 返回 400
        RequestMatcher endpointsMatcher = new AndRequestMatcher(
                authorizationServerConfigurer.getEndpointsMatcher(),
                new NegatedRequestMatcher(new OrRequestMatcher(
                        new AntPathRequestMatcher("/connect/logout"),
                        new AntPathRequestMatcher("/logout/oidc")
                ))
        );
        http.securityMatcher(endpointsMatcher);

        http.csrf(csrf -> csrf.ignoringRequestMatchers(
                "/oauth2/token",
                "/oauth2/introspect",
                "/oauth2/revoke",
                "/oauth2/device_authorization"
        ));

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .clientAuthentication(clientAuthentication -> clientAuthentication
                        .authenticationConverters(converters ->
                                converters.add(0, new DevicePublicClientAuthenticationConverter()))
                        .authenticationProviders(providers ->
                                providers.add(0, new DevicePublicClientAuthenticationProvider(registeredClientRepository))))
                .authorizationEndpoint(endpoint -> endpoint.consentPage("/oauth2/consent"))
                .oidc(oidc -> oidc
                        .userInfoEndpoint(userInfo -> userInfo
                                .userInfoMapper(context -> {
                                    var principal = context.getAuthorization().getPrincipalName();
                                    Map<String, Object> claims = new HashMap<>();
                                    claims.put("sub", principal);
                                    return new OidcUserInfo(claims);
                                })
                        )
                );

        http.exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                )
        );

        return http.build();
    }

    @Bean
    @DependsOn("jwtSigningKeyService")
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        JWKSet jwkSet = jwtSigningKeyService.buildJwkSet();
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtSigningKeyService jwtSigningKeyService) throws Exception {
        JWKSet signingSet = new JWKSet(jwtSigningKeyService.activeSigningKey());
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(signingSet));
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource, TokenService tokenService) {
        JwtDecoder delegate = OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
        return new RevokedTokenJwtDecoder(delegate, tokenService);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(ssoProperties.getIssuer())
                .build();
    }
}
