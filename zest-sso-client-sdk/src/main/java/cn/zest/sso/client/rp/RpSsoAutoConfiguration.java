package cn.zest.sso.client.rp;

import cn.zest.sso.client.rp.oidc.OidcEndpointResolver;
import cn.zest.sso.client.rp.oidc.OidcJwtValidator;
import cn.zest.sso.client.rp.oidc.OidcTokenClient;
import cn.zest.sso.client.rp.provider.DisabledRpProvider;
import cn.zest.sso.client.rp.provider.GenericOidcRpProvider;
import cn.zest.sso.client.rp.provider.ZestSsoRpProvider;
import cn.zest.sso.client.rp.store.InMemoryRpPkceStore;
import cn.zest.sso.client.rp.store.RpPkceStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Zest 生态 RP SSO 统一 SPI 自动配置。
 * <p>接入方提供 {@link RpSsoProperties} Bean 后自动注册 OIDC 流程与提供方 registry。</p>
 */
@AutoConfiguration
@AutoConfigureAfter(cn.zest.sso.client.ZestSsoClientAutoConfiguration.class)
@ConditionalOnProperty(prefix = "zest.sso.client", name = "enabled", havingValue = "true")
public class RpSsoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RpPkceStore.class)
    public RpPkceStore rpPkceStore() {
        return new InMemoryRpPkceStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public OidcEndpointResolver oidcEndpointResolver(RestClient.Builder restClientBuilder,
                                                     ObjectMapper objectMapper) {
        return new OidcEndpointResolver(restClientBuilder, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public OidcTokenClient oidcTokenClient(RestClient.Builder restClientBuilder,
                                           ObjectMapper objectMapper) {
        return new OidcTokenClient(restClientBuilder, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public OidcJwtValidator oidcJwtValidator(ObjectMapper objectMapper,
                                             RestClient.Builder restClientBuilder) {
        return new OidcJwtValidator(objectMapper, restClientBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public DisabledRpProvider disabledRpProvider() {
        return new DisabledRpProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public ZestSsoRpProvider zestSsoRpProvider(RpPkceStore pkceStore,
                                               OidcEndpointResolver endpointResolver,
                                               OidcTokenClient tokenClient,
                                               OidcJwtValidator jwtValidator,
                                               RestClient.Builder restClientBuilder,
                                               ObjectMapper objectMapper) {
        return new ZestSsoRpProvider(pkceStore, endpointResolver, tokenClient, jwtValidator,
                restClientBuilder, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public GenericOidcRpProvider genericOidcRpProvider(RpPkceStore pkceStore,
                                                       OidcEndpointResolver endpointResolver,
                                                       OidcTokenClient tokenClient,
                                                       OidcJwtValidator jwtValidator) {
        return new GenericOidcRpProvider(pkceStore, endpointResolver, tokenClient, jwtValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public RpSsoProviderRegistry rpSsoProviderRegistry(RpSsoProperties properties,
                                                       List<RpSsoProvider> providers,
                                                       DisabledRpProvider disabledProvider) {
        return new RpSsoProviderRegistry(properties, providers, disabledProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public RpSsoAuthService rpSsoAuthService(RpSsoProperties properties,
                                             RpSsoProviderRegistry providerRegistry) {
        return new RpSsoAuthService(properties, providerRegistry);
    }
}
