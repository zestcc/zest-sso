package cn.zest.sso.server.config;

import cn.zest.sso.server.security.SsoUserDetailsService;
import cn.zest.sso.server.service.LdapProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AuthenticationProviderConfig {

    private final SsoUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final LdapProviderService ldapProviderService;

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider daoAuthenticationProvider) {
        List<LdapAuthenticationProvider> ldapProviders = ldapProviderService.buildAuthenticationProviders();
        List<org.springframework.security.authentication.AuthenticationProvider> providers = new ArrayList<>();
        providers.add(daoAuthenticationProvider);
        providers.addAll(ldapProviders);
        return new ProviderManager(providers);
    }
}
