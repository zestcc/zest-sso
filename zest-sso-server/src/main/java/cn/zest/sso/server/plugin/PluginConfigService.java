package cn.zest.sso.server.plugin;

import cn.zest.sso.plugin.PluginRuntimeContext;
import cn.zest.sso.server.domain.entity.SsoPluginConfig;
import cn.zest.sso.server.domain.mapper.SsoPluginConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PluginConfigService implements PluginRuntimeContext {

    private final SsoPluginConfigMapper pluginConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean isPluginEnabled(String pluginKey) {
        SsoPluginConfig row = findByKey(pluginKey);
        return row != null && row.getEnabled() != null && row.getEnabled() == 1;
    }

    @Override
    public Map<String, String> getPluginConfig(String pluginKey) {
        SsoPluginConfig row = findByKey(pluginKey);
        if (row == null || !StringUtils.hasText(row.getConfig())) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(row.getConfig(), new TypeReference<>() {});
            return parsed != null ? parsed : Map.of();
        } catch (Exception e) {
            return Map.of();
        }
    }

    public SsoPluginConfig findByKey(String pluginKey) {
        return pluginConfigMapper.selectOne(new LambdaQueryWrapper<SsoPluginConfig>()
                .eq(SsoPluginConfig::getPluginKey, pluginKey));
    }

    public Map<String, String> parseConfigJson(String json) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(json, new TypeReference<>() {});
            return parsed != null ? new LinkedHashMap<>(parsed) : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public String toConfigJson(Map<String, String> config) {
        if (config == null || config.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            return null;
        }
    }

    public java.util.List<SsoPluginConfig> listAll() {
        return pluginConfigMapper.selectList(new LambdaQueryWrapper<SsoPluginConfig>()
                .orderByAsc(SsoPluginConfig::getPluginKey));
    }

    public void save(String pluginKey, boolean enabled, Map<String, String> config) {
        SsoPluginConfig existing = findByKey(pluginKey);
        if (existing == null) {
            SsoPluginConfig created = new SsoPluginConfig();
            created.setPluginKey(pluginKey);
            created.setEnabled(enabled ? 1 : 0);
            created.setConfig(toConfigJson(config));
            pluginConfigMapper.insert(created);
            return;
        }
        existing.setEnabled(enabled ? 1 : 0);
        if (config != null) {
            Map<String, String> merged = parseConfigJson(existing.getConfig());
            merged.putAll(config);
            existing.setConfig(toConfigJson(merged));
        }
        pluginConfigMapper.updateById(existing);
    }

    public Map<String, String> maskSecrets(Map<String, String> config) {
        if (config == null) {
            return Collections.emptyMap();
        }
        Map<String, String> masked = new LinkedHashMap<>(config);
        for (String key : masked.keySet()) {
            if (key.toLowerCase().contains("secret") || key.toLowerCase().contains("password")) {
                String v = masked.get(key);
                if (StringUtils.hasText(v)) {
                    masked.put(key, "******");
                }
            }
        }
        return masked;
    }
}
