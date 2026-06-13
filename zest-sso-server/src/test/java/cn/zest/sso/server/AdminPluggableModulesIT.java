package cn.zest.sso.server;

import cn.zest.sso.server.config.TestRedisConfig;
import cn.zest.sso.server.support.RequiresMysql;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 可插拔模块 / 插件 / 告警通道 Admin API 生产验收。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RequiresMysql
@Import(TestRedisConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminPluggableModulesIT {

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpSession session;
    private static Long alertChannelId;

    private MockHttpSession adminSession() throws Exception {
        if (session != null) {
            return session;
        }
        session = new MockHttpSession();
        mockMvc.perform(post("/api/admin/auth/login")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        return session;
    }

    @Test
    @Order(1)
    void shouldListMfaAndAlertChannelDescriptors() throws Exception {
        mockMvc.perform(get("/api/admin/channels/mfa").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].key", hasItem("totp")))
                .andExpect(jsonPath("$.data[*].key", hasItem("email")));

        mockMvc.perform(get("/api/admin/channels/alerts").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[*].key", hasItem("http-webhook")))
                .andExpect(jsonPath("$.data[*].key", hasItem("dingtalk-bot")))
                .andExpect(jsonPath("$.data[*].key", hasItem("wecom-bot")));
    }

    @Test
    @Order(2)
    void shouldListSmsPluginsAsNotInstalledByDefault() throws Exception {
        mockMvc.perform(get("/api/admin/plugins").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.pluginKey=='aliyun-sms')].installed").value(false))
                .andExpect(jsonPath("$.data[?(@.pluginKey=='tencent-sms')].installed").value(false));
    }

    @Test
    @Order(3)
    void shouldSavePluginConfigForFutureInstall() throws Exception {
        mockMvc.perform(put("/api/admin/plugins/aliyun-sms")
                        .session(adminSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "config": {
                                    "accessKeyId": "test-ak",
                                    "accessKeySecret": "test-sk",
                                    "signName": "ZestSSO",
                                    "templateCode": "SMS_001"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pluginKey").value("aliyun-sms"))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.config.accessKeySecret").value("******"));

        mockMvc.perform(get("/api/admin/plugins").session(adminSession()))
                .andExpect(jsonPath("$.data[?(@.pluginKey=='aliyun-sms')].enabled").value(true));
    }

    @Test
    @Order(4)
    void shouldRejectUnknownPluginKey() throws Exception {
        mockMvc.perform(put("/api/admin/plugins/unknown-plugin")
                        .session(adminSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"config\":{}}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void shouldCompleteAlertChannelLifecycle() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/admin/alert-channels")
                        .session(adminSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "验收 Webhook",
                                  "channelKey": "http-webhook",
                                  "enabled": 1,
                                  "events": ["LOGIN_SUCCESS"],
                                  "config": {
                                    "url": "http://127.0.0.1:19999/alert-hook"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value("验收 Webhook"))
                .andExpect(jsonPath("$.data.channelKey").value("http-webhook"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();

        Object id = JsonPath.read(create.getResponse().getContentAsString(), "$.data.id");
        alertChannelId = ((Number) id).longValue();

        mockMvc.perform(get("/api/admin/alert-channels").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name=='验收 Webhook')].channelKey").value("http-webhook"));

        mockMvc.perform(put("/api/admin/alert-channels/" + alertChannelId)
                        .session(adminSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "验收 Webhook 更新",
                                  "enabled": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("验收 Webhook 更新"))
                .andExpect(jsonPath("$.data.enabled").value(0));

        mockMvc.perform(delete("/api/admin/alert-channels/" + alertChannelId).session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @Order(6)
    void shouldRejectPluggableAdminApiWithoutSession() throws Exception {
        mockMvc.perform(get("/api/admin/plugins"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/alert-channels"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    void shouldServeAdminStaticIndex() throws Exception {
        mockMvc.perform(get("/admin/"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.containsString("ZestSSO Admin")));
    }
}
