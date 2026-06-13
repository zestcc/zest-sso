package cn.zest.sso.server.config;

import cn.zest.sso.server.security.SsoUserDetails;
import cn.zest.sso.server.security.jackson.SsoUserDetailsMixin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * OAuth2 授权状态 JDBC 持久化，支持多实例 HA。
 */
@Configuration
public class OAuth2AuthorizationStoreConfig {

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        JdbcOAuth2AuthorizationService service =
                new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        service.setAuthorizationRowMapper(
                new SsoOAuth2AuthorizationRowMapper(registeredClientRepository));
        return service;
    }

    private static final class SsoOAuth2AuthorizationRowMapper
            extends JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper {

        private SsoOAuth2AuthorizationRowMapper(RegisteredClientRepository registeredClientRepository) {
            super(registeredClientRepository);
            getObjectMapper().addMixIn(SsoUserDetails.class, SsoUserDetailsMixin.class);
        }
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }
}
