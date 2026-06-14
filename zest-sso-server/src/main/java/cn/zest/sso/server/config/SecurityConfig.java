package cn.zest.sso.server.config;

import cn.zest.sso.server.handler.AdminSecurityHandlers;
import cn.zest.sso.server.security.FormLoginMfaSuccessHandler;
import cn.zest.sso.server.security.FederatedLoginSuccessHandler;
import cn.zest.sso.server.security.LoginRateLimitFilter;
import cn.zest.sso.server.security.SsoLogoutHandler;
import cn.zest.sso.server.federation.oauth.FederatedDelegatingAccessTokenResponseClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final RequestMatcher AUTHORIZATION_SERVER_ENDPOINTS = new OrRequestMatcher(
            new AntPathRequestMatcher("/oauth2/authorize"),
            new AntPathRequestMatcher("/oauth2/token"),
            new AntPathRequestMatcher("/oauth2/introspect"),
            new AntPathRequestMatcher("/oauth2/revoke"),
            new AntPathRequestMatcher("/oauth2/device_authorization"),
            new AntPathRequestMatcher("/oauth2/device_verification"),
            new AntPathRequestMatcher("/oauth2/jwks"),
            new AntPathRequestMatcher("/oauth2/consent"),
            new AntPathRequestMatcher("/userinfo"),
            new AntPathRequestMatcher("/.well-known/openid-configuration")
    );

    private final SsoProperties ssoProperties;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final AdminSecurityHandlers adminSecurityHandlers;
    private final FederatedLoginSuccessHandler federatedLoginSuccessHandler;
    private final FormLoginMfaSuccessHandler formLoginMfaSuccessHandler;
    private final FederatedDelegatingAccessTokenResponseClient federatedAccessTokenResponseClient;
    private final Environment environment;
    private final SsoLogoutHandler ssoLogoutHandler;

    public SecurityConfig(SsoProperties ssoProperties,
                          LoginRateLimitFilter loginRateLimitFilter,
                          AdminSecurityHandlers adminSecurityHandlers,
                          FederatedLoginSuccessHandler federatedLoginSuccessHandler,
                          @Lazy FormLoginMfaSuccessHandler formLoginMfaSuccessHandler,
                          FederatedDelegatingAccessTokenResponseClient federatedAccessTokenResponseClient,
                          Environment environment,
                          @Lazy SsoLogoutHandler ssoLogoutHandler) {
        this.ssoProperties = ssoProperties;
        this.loginRateLimitFilter = loginRateLimitFilter;
        this.adminSecurityHandlers = adminSecurityHandlers;
        this.federatedLoginSuccessHandler = federatedLoginSuccessHandler;
        this.formLoginMfaSuccessHandler = formLoginMfaSuccessHandler;
        this.federatedAccessTokenResponseClient = federatedAccessTokenResponseClient;
        this.environment = environment;
        this.ssoLogoutHandler = ssoLogoutHandler;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(new NegatedRequestMatcher(AUTHORIZATION_SERVER_ENDPOINTS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login", "/login/mfa", "/logout", "/connect/logout", "/logout/oidc",
                                "/forgot-password", "/reset-password",
                                "/api/public/password/**",
                                "/api/public/webauthn/**",
                                "/login/oauth2/**",
                                "/saml2/**", "/login/saml2/**",
                                "/css/**", "/js/**", "/images/**", "/admin/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/actuator/health",
                                "/api/public/**",
                                "/api/admin/auth/login",
                                "/api/admin/auth/mfa/verify"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasAnyRole("SSO_ADMIN", "SSO_OPERATOR", "TENANT_ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(adminSecurityHandlers)
                        .accessDeniedHandler(adminSecurityHandlers))
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .tokenEndpoint(token -> token.accessTokenResponseClient(federatedAccessTokenResponseClient))
                        .successHandler(federatedLoginSuccessHandler))
                .saml2Login(saml2 -> saml2
                        .loginPage("/login")
                        .successHandler(federatedLoginSuccessHandler))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(formLoginMfaSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new OrRequestMatcher(
                                new AntPathRequestMatcher("/logout", "GET"),
                                new AntPathRequestMatcher("/logout", "POST")))
                        .addLogoutHandler(ssoLogoutHandler)
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("SESSION", "JSESSIONID")
                        .permitAll()
                )
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        if (isDevProfile()) {
            // 本地 Admin Vite 端口被占用时会自动换端口（如 5180），dev 放行 localhost / 127.0.0.1
            config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        } else {
            config.setAllowedOrigins(ssoProperties.getSecurity().getCorsAllowedOrigins());
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private boolean isDevProfile() {
        return !Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equals);
    }
}
