package cn.zest.sso.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@AutoConfiguration
@EnableConfigurationProperties(ZestSsoClientProperties.class)
@ConditionalOnProperty(prefix = "zest.sso.client", name = "enabled", havingValue = "true")
@ComponentScan(basePackageClasses = {
        ZestSsoOidcClient.class,
        ZestSsoBackchannelLogoutController.class,
        ZestSsoFrontchannelLogoutController.class,
        ZestSsoLogoutTokenValidator.class
})
public class ZestSsoClientAutoConfiguration {

    @Bean
    public JwtDecoder zestSsoJwtDecoder(ZestSsoClientProperties properties) {
        String jwksUri = properties.getIssuer().replaceAll("/$", "") + "/oauth2/jwks";
        return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }
}
