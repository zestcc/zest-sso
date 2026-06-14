package cn.zest.sso.server;

import cn.zest.sso.server.config.TestRedisConfig;
import cn.zest.sso.server.support.RequiresMysql;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RequiresMysql
@Import(TestRedisConfig.class)
class OidcPublicApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeOidcDiscovery() throws Exception {
        mockMvc.perform(get("/api/public/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:9000"))
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists())
                .andExpect(jsonPath("$.end_session_endpoint").value("http://localhost:9000/connect/logout"));
    }

    @Test
    void shouldExposeJwks() throws Exception {
        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray());
    }

    @Test
    void shouldExposeHealth() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value("UP"));
    }

    @Test
    void shouldRenderLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(containsString("ZestSSO")));
    }

    @Test
    void shouldBuildLogoutUrl() throws Exception {
        mockMvc.perform(get("/api/public/logout-url")
                        .param("redirect_uri", "http://localhost:5173/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(containsString("connect/logout")));
    }

    @Test
    void shouldOidcLogoutWithoutRequiringAuthorizationServerAuth() throws Exception {
        mockMvc.perform(get("/connect/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/login?logout"));
    }
}
