package cn.zest.sso.server.federation.adapter;

import cn.zest.sso.server.domain.dto.CreateIdentityProviderRequest;
import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.federation.spi.FederatedIdpAdapterDescriptor;
import cn.zest.sso.server.federation.spi.FederatedIdpEndpointConfig;
import cn.zest.sso.server.federation.support.OAuth2ClientRegistrationSupport;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 企业微信 — 预置手动端点；完整换票需阶段 B 自定义 TokenResponseClient（descriptor.productionReady=false）。
 */
@Component
public class WecomFederatedIdpAdapter extends AbstractOAuth2FederatedIdpAdapter {

    public WecomFederatedIdpAdapter(OAuth2ClientRegistrationSupport registrationSupport) {
        super(registrationSupport);
    }

    @Override
    public String adapterKey() {
        return "wecom";
    }

    @Override
    public FederatedIdpAdapterDescriptor descriptor() {
        return new FederatedIdpAdapterDescriptor(
                adapterKey(),
                "企业微信",
                "企业微信 OAuth2（手动端点）；生产换票需启用 wecom 定制 Token 客户端（阶段 B）",
                false,
                true,
                false,
                Map.of("usernameClaim", "userid", "emailClaim", "email", "displayNameClaim", "name"),
                Map.of(
                        "authorizationUri", "https://open.weixin.qq.com/connect/oauth2/authorize",
                        "authorizationQueryParams", "response_type=code&scope=snsapi_privateinfo#wechat_redirect"
                )
        );
    }

    @Override
    protected FederatedIdpEndpointConfig defaultEndpoints() {
        FederatedIdpEndpointConfig config = new FederatedIdpEndpointConfig();
        config.setAuthorizationUri("https://open.weixin.qq.com/connect/oauth2/authorize");
        config.setAuthorizationQueryParams("response_type=code&scope=snsapi_privateinfo#wechat_redirect");
        // tokenUri 因企微协议非标准 OAuth2，阶段 B 由 WecomOAuth2TokenResponseClient 提供
        return config;
    }

    @Override
    public void validateCreate(CreateIdentityProviderRequest request) {
        requireClientCredentials(request);
        boolean hasManual = request.getEndpointConfig() != null
                && StringUtils.hasText(request.getEndpointConfig().getAuthorizationUri());
        if (!hasManual && (request.getEndpointConfig() == null || !StringUtils.hasText(request.getEndpointConfig().getTokenUri()))) {
            // 允许仅配置授权端点用于扫码入口试点；完整登录需 tokenUri
            requireDiscoveryOrManual(request);
        }
    }

    @Override
    public void applyDefaults(SsoIdentityProvider provider) {
        if (!StringUtils.hasText(provider.getScopes())) {
            provider.setScopes("snsapi_privateinfo");
        }
        if (!StringUtils.hasText(provider.getUsernameClaim())) {
            provider.setUsernameClaim("userid");
        }
    }
}
