package cn.zest.sso.server.security;

import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.domain.mapper.SsoIdentityProviderMapper;
import cn.zest.sso.server.federation.FederatedIdpAdapterRegistry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseClientRegistrationRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final SsoIdentityProviderMapper identityProviderMapper;
    private final FederatedIdpAdapterRegistry adapterRegistry;

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        SsoIdentityProvider provider = identityProviderMapper.selectOne(new LambdaQueryWrapper<SsoIdentityProvider>()
                .eq(SsoIdentityProvider::getAlias, registrationId)
                .eq(SsoIdentityProvider::getProviderType, "OIDC")
                .eq(SsoIdentityProvider::getEnabled, 1));
        if (provider == null) {
            return null;
        }
        return adapterRegistry.buildClientRegistration(provider);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        List<ClientRegistration> registrations = new ArrayList<>();
        identityProviderMapper.selectList(new LambdaQueryWrapper<SsoIdentityProvider>()
                        .eq(SsoIdentityProvider::getProviderType, "OIDC")
                        .eq(SsoIdentityProvider::getEnabled, 1))
                .forEach(provider -> {
                    try {
                        registrations.add(adapterRegistry.buildClientRegistration(provider));
                    } catch (Exception ignored) {
                        // 未就绪的适配器（如 wecom 缺 token）跳过注册，避免拖垮整链
                    }
                });
        return registrations.iterator();
    }
}
