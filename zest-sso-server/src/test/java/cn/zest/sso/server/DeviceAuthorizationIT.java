package cn.zest.sso.server;

import cn.zest.sso.server.config.TestRedisConfig;
import cn.zest.sso.server.security.DatabaseRegisteredClientRepository;
import cn.zest.sso.server.support.RequiresMysql;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RequiresMysql
@Import(TestRedisConfig.class)
class DeviceAuthorizationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseRegisteredClientRepository clientRepository;

    @Test
    void shouldExposeDeviceAuthorizationInDiscovery() throws Exception {
        mockMvc.perform(get("/api/public/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.device_authorization_endpoint").value(notNullValue()));
    }

    @Test
    void shouldIssueDeviceCodeForPublicClient() throws Exception {
        mockMvc.perform(post("/oauth2/device_authorization")
                        .contentType("application/x-www-form-urlencoded")
                        .param("client_id", "device-cli")
                        .param("scope", "openid profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.device_code").value(notNullValue()))
                .andExpect(jsonPath("$.user_code").value(notNullValue()))
                .andExpect(jsonPath("$.verification_uri").value(notNullValue()));
    }

    @Test
    void shouldRegisterDeviceCliClientWithDeviceGrant() {
        RegisteredClient client = clientRepository.findByClientId("device-cli");
        assertThat(client).isNotNull();
        assertThat(client.getAuthorizationGrantTypes())
                .contains(new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:device_code"));
        assertThat(client.getClientAuthenticationMethods())
                .containsOnly(org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE);
    }
}
