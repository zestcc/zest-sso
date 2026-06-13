package cn.zest.sso.server.modules.builtin;

import cn.zest.sso.server.alert.AlertChannelRegistry;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.federation.FederatedIdpAdapterRegistry;
import cn.zest.sso.server.mfa.MfaChannelRegistry;
import cn.zest.sso.server.modules.spi.OptionalModule;
import cn.zest.sso.server.modules.spi.OptionalModuleDescriptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
class FederationOptionalModule implements OptionalModule {

    private final SsoProperties ssoProperties;
    private final FederatedIdpAdapterRegistry adapterRegistry;

    @Override
    public String moduleKey() {
        return "federation";
    }

    @Override
    public OptionalModuleDescriptor descriptor() {
        return new OptionalModuleDescriptor(
                moduleKey(), "身份联邦", "OIDC/SAML 联邦登录与 domestic IdP 适配器",
                "auth", ssoProperties.getModules().isFederation(), true,
                Map.of("adapters", String.valueOf(adapterRegistry.listDescriptors().size())));
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getModules().isFederation();
    }
}

@Component
@RequiredArgsConstructor
class SmsMfaOptionalModule implements OptionalModule {

    private final SsoProperties ssoProperties;
    private final MfaChannelRegistry mfaChannelRegistry;

    @Override
    public String moduleKey() {
        return "sms-mfa";
    }

    @Override
    public OptionalModuleDescriptor descriptor() {
        long enabled = mfaChannelRegistry.listDescriptors().stream().filter(d -> d.enabled()).count();
        return new OptionalModuleDescriptor(
                moduleKey(), "短信 MFA", "阿里云/腾讯云短信 step-up（可选）",
                "security", ssoProperties.getModules().isSmsMfa() && enabled > 0, true,
                Map.of("channels", "aliyun-sms,tencent-sms"));
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getModules().isSmsMfa();
    }
}

@Component
@RequiredArgsConstructor
class AlertsOptionalModule implements OptionalModule {

    private final SsoProperties ssoProperties;
    private final AlertChannelRegistry alertChannelRegistry;

    @Override
    public String moduleKey() {
        return "alerts";
    }

    @Override
    public OptionalModuleDescriptor descriptor() {
        return new OptionalModuleDescriptor(
                moduleKey(), "告警通知", "HTTP/钉钉/企微机器人 IAM 事件推送",
                "ops", ssoProperties.getModules().isAlerts() && ssoProperties.getAlerts().isEnabled(), true,
                Map.of("channels", String.valueOf(alertChannelRegistry.listDescriptors().size())));
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getModules().isAlerts() && ssoProperties.getAlerts().isEnabled();
    }
}

@Component
@RequiredArgsConstructor
class WecomFederationOptionalModule implements OptionalModule {

    private final SsoProperties ssoProperties;

    @Override
    public String moduleKey() {
        return "wecom-federation";
    }

    @Override
    public OptionalModuleDescriptor descriptor() {
        return new OptionalModuleDescriptor(
                moduleKey(), "企微联邦", "企业微信 OAuth2 换票客户端",
                "auth", ssoProperties.getModules().isWecomFederation(), true,
                Map.of("adapterKey", "wecom"));
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getModules().isWecomFederation();
    }
}

@Component
@RequiredArgsConstructor
class ClientOnboardingOptionalModule implements OptionalModule {

    private final SsoProperties ssoProperties;

    @Override
    public String moduleKey() {
        return "client-onboarding";
    }

    @Override
    public OptionalModuleDescriptor descriptor() {
        return new OptionalModuleDescriptor(
                moduleKey(), "应用接入向导", "OIDC 客户端一键创建与集成片段",
                "integration", ssoProperties.getModules().isClientOnboardingWizard(), false, Map.of());
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getModules().isClientOnboardingWizard();
    }
}

@Component
@RequiredArgsConstructor
class WebhooksOptionalModule implements OptionalModule {

    private final SsoProperties ssoProperties;

    @Override
    public String moduleKey() {
        return "webhooks";
    }

    @Override
    public OptionalModuleDescriptor descriptor() {
        return new OptionalModuleDescriptor(
                moduleKey(), "IAM Webhook", "登录/登出事件 HTTP 投递队列",
                "ops", ssoProperties.getModules().isWebhooks() && ssoProperties.getWebhooks().isEnabled(), true,
                Map.of("endpoints", String.valueOf(
                        ssoProperties.getWebhooks().getEndpoints() != null
                                ? ssoProperties.getWebhooks().getEndpoints().size() : 0)));
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getModules().isWebhooks() && ssoProperties.getWebhooks().isEnabled();
    }
}

@Component
@RequiredArgsConstructor
class ScimOptionalModule implements OptionalModule {

    private final SsoProperties ssoProperties;

    @Override
    public String moduleKey() {
        return "scim";
    }

    @Override
    public OptionalModuleDescriptor descriptor() {
        return new OptionalModuleDescriptor(
                moduleKey(), "SCIM 2.0", "用户/组同步与 Bulk/PATCH",
                "integration", ssoProperties.getModules().isScim(), false, Map.of());
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getModules().isScim();
    }
}

@Component
@RequiredArgsConstructor
class WebauthnOptionalModule implements OptionalModule {

    private final SsoProperties ssoProperties;

    @Override
    public String moduleKey() {
        return "webauthn";
    }

    @Override
    public OptionalModuleDescriptor descriptor() {
        return new OptionalModuleDescriptor(
                moduleKey(), "WebAuthn / Passkey", "无密码登录",
                "security", ssoProperties.getModules().isWebauthn()
                        && ssoProperties.getWebauthn().isEnabled(), true, Map.of());
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getModules().isWebauthn() && ssoProperties.getWebauthn().isEnabled();
    }
}
