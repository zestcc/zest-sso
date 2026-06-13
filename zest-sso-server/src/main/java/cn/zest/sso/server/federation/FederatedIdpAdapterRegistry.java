package cn.zest.sso.server.federation;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.federation.spi.FederatedIdpAdapter;
import cn.zest.sso.server.federation.spi.FederatedIdpAdapterDescriptor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FederatedIdpAdapterRegistry {

    private final Map<String, FederatedIdpAdapter> adaptersByKey;
    private final FederatedIdpAdapter defaultAdapter;

    public FederatedIdpAdapterRegistry(List<FederatedIdpAdapter> adapters) {
        this.adaptersByKey = adapters.stream()
                .collect(Collectors.toMap(FederatedIdpAdapter::adapterKey, Function.identity(), (a, b) -> a));
        this.defaultAdapter = adaptersByKey.get("generic-oidc");
        if (defaultAdapter == null) {
            throw new IllegalStateException("缺少默认适配器 generic-oidc");
        }
    }

    public FederatedIdpAdapter resolve(SsoIdentityProvider provider) {
        if (provider == null) {
            return defaultAdapter;
        }
        String key = provider.getAdapterKey();
        if (key == null || key.isBlank()) {
            return defaultAdapter;
        }
        FederatedIdpAdapter adapter = adaptersByKey.get(key);
        if (adapter == null) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "未知身份源适配器: " + key);
        }
        if (!adapter.supports(provider)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "适配器 " + key + " 不支持当前身份源配置");
        }
        return adapter;
    }

    public ClientRegistration buildClientRegistration(SsoIdentityProvider provider) {
        return resolve(provider).buildClientRegistration(provider);
    }

    public List<FederatedIdpAdapterDescriptor> listDescriptors() {
        return adaptersByKey.values().stream()
                .map(FederatedIdpAdapter::descriptor)
                .sorted(Comparator.comparing(FederatedIdpAdapterDescriptor::key))
                .toList();
    }
}
