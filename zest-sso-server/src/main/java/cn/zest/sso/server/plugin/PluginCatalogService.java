package cn.zest.sso.server.plugin;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.plugin.PluggablePlugin;
import cn.zest.sso.plugin.alert.AlertChannelAdapter;
import cn.zest.sso.plugin.mfa.MfaChannelAdapter;
import cn.zest.sso.server.domain.entity.SsoPluginConfig;
import cn.zest.sso.server.domain.vo.PluginConfigVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PluginCatalogService {

    private static final List<PluginCatalogEntry> KNOWN_PLUGINS = List.of(
            new PluginCatalogEntry("aliyun-sms", "阿里云短信", "mfa",
                    Map.of("accessKeyId", "AccessKey ID", "accessKeySecret", "AccessKey Secret",
                            "signName", "短信签名", "templateCode", "模板编号", "regionId", "区域")),
            new PluginCatalogEntry("tencent-sms", "腾讯云短信", "mfa",
                    Map.of("secretId", "SecretId", "secretKey", "SecretKey",
                            "sdkAppId", "SdkAppId", "signName", "签名", "templateId", "模板 ID", "region", "区域"))
    );

    private final List<MfaChannelAdapter> mfaAdapters;
    private final List<AlertChannelAdapter> alertAdapters;
    private final PluginConfigService pluginConfigService;

    public List<PluginConfigVO> listPluginConfigs() {
        Map<String, PluggablePlugin> installed = new LinkedHashMap<>();
        mfaAdapters.forEach(a -> installed.put(a.pluginKey(), a));
        alertAdapters.forEach(a -> installed.put(a.pluginKey(), a));

        Map<String, SsoPluginConfig> saved = pluginConfigService.listAll().stream()
                .collect(Collectors.toMap(SsoPluginConfig::getPluginKey, Function.identity(), (a, b) -> a));

        List<PluginConfigVO> result = new ArrayList<>();
        for (PluginCatalogEntry known : KNOWN_PLUGINS) {
            PluggablePlugin plugin = installed.get(known.key());
            SsoPluginConfig row = saved.get(known.key());
            Map<String, String> config = row != null
                    ? pluginConfigService.maskSecrets(pluginConfigService.parseConfigJson(row.getConfig()))
                    : Map.of();
            result.add(PluginConfigVO.builder()
                    .pluginKey(known.key())
                    .pluginName(known.name())
                    .category(known.category())
                    .installed(plugin != null)
                    .enabled(row != null && row.getEnabled() != null && row.getEnabled() == 1)
                    .configured(isConfigured(known.key(), config))
                    .configHints(known.hints())
                    .config(config)
                    .build());
        }
        result.sort(Comparator.comparing(PluginConfigVO::getCategory).thenComparing(PluginConfigVO::getPluginKey));
        return result;
    }

    public PluginConfigVO savePluginConfig(String pluginKey, boolean enabled, Map<String, String> config) {
        boolean known = KNOWN_PLUGINS.stream().anyMatch(p -> p.key().equals(pluginKey));
        if (!known) {
            throw new SsoException(ErrorCode.NOT_FOUND, "未知插件: " + pluginKey);
        }
        pluginConfigService.save(pluginKey, enabled, config);
        return listPluginConfigs().stream()
                .filter(p -> pluginKey.equals(p.getPluginKey()))
                .findFirst()
                .orElseThrow();
    }

    private boolean isConfigured(String pluginKey, Map<String, String> config) {
        if ("aliyun-sms".equals(pluginKey)) {
            return has(config, "accessKeyId", "accessKeySecret", "signName", "templateCode");
        }
        if ("tencent-sms".equals(pluginKey)) {
            return has(config, "secretId", "secretKey", "sdkAppId", "signName", "templateId");
        }
        return false;
    }

    private boolean has(Map<String, String> config, String... keys) {
        for (String key : keys) {
            if (!StringUtils.hasText(config.get(key))) {
                return false;
            }
        }
        return true;
    }

    private record PluginCatalogEntry(String key, String name, String category, Map<String, String> hints) {
    }
}
