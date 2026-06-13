package cn.zest.sso.server.security.jackson;

import cn.zest.sso.server.security.SsoUserDetails;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SsoUserDetailsDeserializer extends JsonDeserializer<SsoUserDetails> {

    @Override
    public SsoUserDetails deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        Long userId = node.hasNonNull("userId") ? node.get("userId").asLong() : null;
        String username = text(node, "username");
        String password = text(node, "password");
        String email = text(node, "email");
        String displayName = text(node, "displayName");
        boolean enabled = !node.has("enabled") || node.get("enabled").asBoolean();
        boolean accountNonLocked = !node.has("accountNonLocked") || node.get("accountNonLocked").asBoolean();
        boolean superAdmin = node.has("superAdmin") && node.get("superAdmin").asBoolean();
        List<String> roles = readStringList(node, "roles");
        List<String> groups = readStringList(node, "groups");
        Long defaultTenantId = node.hasNonNull("defaultTenantId") ? node.get("defaultTenantId").asLong() : null;
        String defaultTenantCode = text(node, "defaultTenantCode");
        return new SsoUserDetails(userId, username, password, email, displayName, enabled,
                accountNonLocked, superAdmin, roles, groups, defaultTenantId, defaultTenantCode);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && !value.isNull() ? value.asText() : null;
    }

    private static List<String> readStringList(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        value.forEach(item -> result.add(item.asText()));
        return result;
    }
}
