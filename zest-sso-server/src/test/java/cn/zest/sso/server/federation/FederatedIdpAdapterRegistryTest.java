package cn.zest.sso.server.federation;

import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.federation.adapter.DingtalkFederatedIdpAdapter;
import cn.zest.sso.server.federation.adapter.FeishuFederatedIdpAdapter;
import cn.zest.sso.server.federation.adapter.GenericOidcFederatedIdpAdapter;
import cn.zest.sso.server.federation.adapter.WecomFederatedIdpAdapter;
import cn.zest.sso.server.federation.support.OAuth2ClientRegistrationSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FederatedIdpAdapterRegistryTest {

    private FederatedIdpAdapterRegistry registry;

    @BeforeEach
    void setUp() {
        OAuth2ClientRegistrationSupport support = new OAuth2ClientRegistrationSupport(new ObjectMapper());
        SsoProperties properties = new SsoProperties();
        registry = new FederatedIdpAdapterRegistry(List.of(
                new GenericOidcFederatedIdpAdapter(support),
                new FeishuFederatedIdpAdapter(support),
                new DingtalkFederatedIdpAdapter(support),
                new WecomFederatedIdpAdapter(support, properties)
        ));
    }

    @Test
    void shouldListDomesticAdapters() {
        assertThat(registry.listDescriptors())
                .extracting(d -> d.key())
                .contains("generic-oidc", "feishu", "dingtalk", "wecom");
    }

    @Test
    void feishuAdapterShouldApplyDefaultDiscovery() {
        SsoIdentityProvider provider = new SsoIdentityProvider();
        provider.setProviderType("OIDC");
        provider.setAdapterKey("feishu");
        provider.setAlias("feishu");
        provider.setClientId("cli_test");
        provider.setClientSecret("secret");
        registry.resolve(provider).applyDefaults(provider);
        assertThat(provider.getDiscoveryUri()).contains("feishu.cn");
    }

    @Test
    void dingtalkAdapterShouldExposeManualEndpointsWhenDiscoveryMissing() {
        SsoIdentityProvider provider = new SsoIdentityProvider();
        provider.setProviderType("OIDC");
        provider.setAdapterKey("dingtalk");
        provider.setAlias("dingtalk");
        provider.setClientId("ding");
        provider.setClientSecret("secret");
        provider.setScopes("openid");
        provider.setEndpointConfig("""
                {"authorizationUri":"https://login.dingtalk.com/oauth2/auth","tokenUri":"https://api.dingtalk.com/v1.0/oauth2/userAccessToken"}
                """);
        ClientRegistration registration = registry.buildClientRegistration(provider);
        assertThat(registration.getRegistrationId()).isEqualTo("dingtalk");
        assertThat(registration.getProviderDetails().getAuthorizationUri())
                .isEqualTo("https://login.dingtalk.com/oauth2/auth");
    }
}
