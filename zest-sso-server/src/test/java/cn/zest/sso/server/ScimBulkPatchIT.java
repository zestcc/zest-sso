package cn.zest.sso.server;

import cn.zest.sso.server.config.TestRedisConfig;
import cn.zest.sso.server.support.RequiresMysql;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RequiresMysql
@Import(TestRedisConfig.class)
class ScimBulkPatchIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String obtainToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/oauth2/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "client_credentials")
                        .param("client_id", "scim-provisioner")
                        .param("client_secret", "change-me-in-production")
                        .param("scope", "scim"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("access_token").asText();
    }

    @Test
    void shouldPatchDeactivateUser() throws Exception {
        String accessToken = obtainToken();
        String userName = "scim-patch-" + System.currentTimeMillis();
        MvcResult createResult = mockMvc.perform(post("/scim/v2/Users")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                                  "userName": "%s",
                                  "displayName": "SCIM Patch User",
                                  "active": true
                                }
                                """.formatted(userName)))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/scim/v2/Users/" + userId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType("application/scim+json")
                        .content("""
                                {
                                  "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                                  "Operations": [{"op": "replace", "path": "active", "value": false}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldProcessBulkCreateAndDelete() throws Exception {
        String accessToken = obtainToken();
        String userName = "scim-bulk-" + System.currentTimeMillis();
        String bulkRequest = """
                {
                  "schemas": ["urn:ietf:params:scim:api:messages:2.0:BulkRequest"],
                  "Operations": [
                    {
                      "method": "POST",
                      "bulkId": "bulk-user-1",
                      "path": "/Users",
                      "data": {
                        "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                        "userName": "%s",
                        "displayName": "SCIM Bulk User",
                        "active": true
                      }
                    }
                  ]
                }
                """.formatted(userName);
        MvcResult bulkResult = mockMvc.perform(post("/scim/v2/Bulk")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Operations", hasSize(1)))
                .andExpect(jsonPath("$.Operations[0].status").value(201))
                .andReturn();

        String location = objectMapper.readTree(bulkResult.getResponse().getContentAsString())
                .get("Operations").get(0).get("location").asText();
        String userId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(post("/scim/v2/Bulk")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "schemas", List.of("urn:ietf:params:scim:api:messages:2.0:BulkRequest"),
                                "Operations", List.of(Map.of(
                                        "method", "DELETE",
                                        "bulkId", "bulk-user-del",
                                        "path", "/Users/" + userId
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Operations[0].status").value(204));
    }

    @Test
    void shouldExposeBulkSupportInServiceProviderConfig() throws Exception {
        mockMvc.perform(get("/scim/v2/ServiceProviderConfig"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bulk.supported").value(true));
    }
}
