package cn.zest.sso.server;

import cn.zest.sso.server.config.TestRedisConfig;
import cn.zest.sso.server.support.RequiresMysql;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理台登录后会话链路：登录 → /me → 受保护 Admin API。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RequiresMysql
@Import(TestRedisConfig.class)
class AdminSessionChainIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAccessProtectedAdminApiAfterLogin() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/admin/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/admin/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));

        mockMvc.perform(get("/api/admin/clients").session(session)
                        .param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray());
    }

    @Test
    void shouldRejectProtectedAdminApiWithoutSession() throws Exception {
        mockMvc.perform(get("/api/admin/clients").param("page", "1").param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCompleteAdminClientLifecycle() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/admin/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk());

        String clientId = "acceptance-" + System.currentTimeMillis();
        MvcResult create = mockMvc.perform(post("/api/admin/clients")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "clientSecret": "acceptance-secret-123",
                                  "clientName": "Acceptance Test",
                                  "redirectUris": ["http://localhost:5173/callback"],
                                  "scopes": ["openid","profile"],
                                  "requirePkce": true
                                }
                                """.formatted(clientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(clientId))
                .andReturn();

        mockMvc.perform(post("/api/admin/clients/" + clientId + "/disable").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/clients/" + clientId + "/enable").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/clients/" + clientId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(clientId));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/admin/clients/" + clientId).session(session))
                .andExpect(status().isOk());
    }
}
