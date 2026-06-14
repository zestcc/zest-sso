package cn.zest.sso.server;

import cn.zest.sso.server.config.TestRedisConfig;
import cn.zest.sso.server.support.BackchannelTestReceiver;
import cn.zest.sso.server.support.RequiresMysql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RequiresMysql
@Import(TestRedisConfig.class)
class BackchannelLogoutE2EIT {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BackchannelTestReceiver testReceiver;

    @BeforeEach
    void prepare() throws Exception {
        testReceiver.clear();
        String callbackUri = "http://localhost:" + port + "/api/public/test/rp/backchannel-logout";
        jdbcTemplate.update(
                "UPDATE sso_oauth_client SET backchannel_logout_uri = ? WHERE client_id = 'backchannel-test-rp'",
                callbackUri);
        mockMvc.perform(post("/api/public/test/backchannel/clear"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDeliverBackchannelLogoutToBuiltinTestRp() throws Exception {
        mockMvc.perform(post("/api/public/test/backchannel/seed")
                        .param("clientId", "backchannel-test-rp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        var loginResult = mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession();

        mockMvc.perform(post("/api/admin/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        BackchannelTestReceiver.Receipt receipt = null;
        for (int i = 0; i < 50; i++) {
            receipt = testReceiver.lastReceipt();
            if (receipt != null) {
                break;
            }
            Thread.sleep(200);
        }
        assertNotNull(receipt, "未收到 Back-Channel logout_token");
        assertEquals("admin", receipt.principalName());
        assertEquals("backchannel-test-rp", receipt.clientId());

        mockMvc.perform(get("/api/public/test/rp/backchannel-logout/last"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.received").value(true))
                .andExpect(jsonPath("$.data.principalName").value("admin"));
    }
}
