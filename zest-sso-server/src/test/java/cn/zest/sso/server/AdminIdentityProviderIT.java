package cn.zest.sso.server;

import cn.zest.sso.server.config.TestRedisConfig;
import cn.zest.sso.server.support.RequiresMysql;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RequiresMysql
@Import(TestRedisConfig.class)
class AdminIdentityProviderIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldParseSamlMetadataForAdmin() throws Exception {
        String metadataUri = new ClassPathResource("saml/test-idp-metadata.xml").getFile().toURI().toString();
        mockMvc.perform(post("/api/admin/identity-providers/parse-saml-metadata")
                        .with(user("admin").roles("SSO_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metadataUri\":\"" + metadataUri + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.entityId").value("https://idp.example.com/metadata"))
                .andExpect(jsonPath("$.data.ssoUrl").value("https://idp.example.com/sso/saml"));
    }

    @Test
    void shouldCreateSamlIdentityProvider() throws Exception {
        String alias = "test-saml-" + System.currentTimeMillis();
        String pem = new String(getClass().getResourceAsStream("/saml/test.crt").readAllBytes())
                .replace("\r", "")
                .replace("\n", "\\n");
        mockMvc.perform(post("/api/admin/identity-providers")
                        .with(user("admin").roles("SSO_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alias": "%s",
                                  "displayName": "Test SAML IdP",
                                  "providerType": "SAML",
                                  "samlEntityId": "https://idp.example.com/metadata",
                                  "samlSsoUrl": "https://idp.example.com/sso/saml",
                                  "samlVerificationCertificate": "%s"
                                }
                                """.formatted(alias, pem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerType").value("SAML"))
                .andExpect(jsonPath("$.data.loginUrl", containsString("/saml2/authenticate/" + alias)));
    }

    @Test
    void shouldListFederationAdapters() throws Exception {
        mockMvc.perform(get("/api/admin/identity-providers/adapters")
                        .with(user("admin").roles("SSO_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.key=='feishu')]").exists())
                .andExpect(jsonPath("$.data[?(@.key=='dingtalk')]").exists());
    }

    @Test
    void shouldCreateFeishuIdentityProviderWithAdapterKey() throws Exception {
        String alias = "feishu-" + System.currentTimeMillis();
        mockMvc.perform(post("/api/admin/identity-providers")
                        .with(user("admin").roles("SSO_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alias": "%s",
                                  "displayName": "飞书登录",
                                  "providerType": "OIDC",
                                  "adapterKey": "feishu",
                                  "clientId": "cli_test",
                                  "clientSecret": "secret_test"
                                }
                                """.formatted(alias)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adapterKey").value("feishu"))
                .andExpect(jsonPath("$.data.discoveryUri", containsString("feishu.cn")))
                .andExpect(jsonPath("$.data.loginUrl", containsString("/oauth2/authorization/" + alias)));
    }

    @Test
    void shouldListGroupsForAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/groups")
                        .with(user("admin").roles("SSO_ADMIN"))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
