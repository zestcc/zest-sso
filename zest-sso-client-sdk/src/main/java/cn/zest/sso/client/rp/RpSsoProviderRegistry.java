package cn.zest.sso.client.rp;

import cn.zest.sso.client.rp.provider.DisabledRpProvider;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 按配置选择 RP SSO 提供方。
 */
public class RpSsoProviderRegistry {

    private final RpSsoProperties properties;
    private final Map<String, RpSsoProvider> providers;
    private final DisabledRpProvider disabledProvider;

    public RpSsoProviderRegistry(RpSsoProperties properties,
                                 List<RpSsoProvider> providerList,
                                 DisabledRpProvider disabledProvider) {
        this.properties = properties;
        this.disabledProvider = disabledProvider;
        this.providers = providerList.stream()
                .filter(p -> !(p instanceof DisabledRpProvider))
                .collect(Collectors.toMap(RpSsoProvider::providerId, Function.identity(), (a, b) -> a));
    }

    public RpSsoProvider resolve() {
        if (!properties.isEnabled() || isDisabledProviderId(properties.getProvider())) {
            return disabledProvider;
        }
        RpSsoProvider provider = providers.get(normalize(properties.getProvider()));
        return provider != null ? provider : disabledProvider;
    }

    private static boolean isDisabledProviderId(String provider) {
        return !StringUtils.hasText(provider) || "none".equalsIgnoreCase(provider.trim());
    }

    private static String normalize(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase();
    }
}
