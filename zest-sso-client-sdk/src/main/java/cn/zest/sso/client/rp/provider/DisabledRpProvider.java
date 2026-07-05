package cn.zest.sso.client.rp.provider;

import cn.zest.sso.client.rp.*;
import org.springframework.stereotype.Component;

/**
 * SSO 关闭时的空实现。
 */
public class DisabledRpProvider implements RpSsoProvider {

    @Override
    public String providerId() {
        return "none";
    }

    @Override
    public RpSsoPublicConfig buildPublicConfig(RpSsoProperties props) {
        return RpSsoPublicConfig.builder()
                .enabled(false)
                .provider("none")
                .displayName("SSO")
                .build();
    }

    @Override
    public RpSsoAuthorizeInfo buildAuthorizeUrl(RpSsoProperties props) {
        throw new RpSsoException("SSO_DISABLED", "SSO 未启用");
    }

    @Override
    public String buildLogoutUrl(RpSsoProperties props) {
        return null;
    }

    @Override
    public RpSsoCallbackResult handleCallback(RpSsoCallbackRequest request, RpSsoProperties props) {
        throw new RpSsoException("SSO_DISABLED", "SSO 未启用");
    }
}
