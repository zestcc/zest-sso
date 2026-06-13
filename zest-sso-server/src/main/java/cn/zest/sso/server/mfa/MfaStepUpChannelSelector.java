package cn.zest.sso.server.mfa;

import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.mfa.spi.MfaChannelAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class MfaStepUpChannelSelector {

    public static final String MODE_TOTP = "TOTP";
    public static final String MODE_EMAIL = "EMAIL";
    public static final String MODE_ALIYUN_SMS = "ALIYUN_SMS";
    public static final String MODE_TENCENT_SMS = "TENCENT_SMS";

    private final SsoProperties ssoProperties;
    private final MfaChannelRegistry channelRegistry;

    public String resolveBoundMode(SsoUser user) {
        if (user.getMfaEnabled() != null && user.getMfaEnabled() == 1) {
            return MODE_TOTP;
        }
        return resolveStepUpMode(user);
    }

    public String resolveStepUpMode(SsoUser user) {
        String priority = ssoProperties.getMfa().getStepUpPriority();
        if (!StringUtils.hasText(priority)) {
            priority = "sms,email";
        }
        for (String token : priority.split(",")) {
            String key = token.trim().toLowerCase();
            if ("sms".equals(key)) {
                String sms = firstEnabledSmsKey(user);
                if (sms != null) {
                    return sms;
                }
            } else if ("email".equals(key) && channelRegistry.resolve("email").isEnabled()) {
                return MODE_EMAIL;
            }
        }
        if (channelRegistry.resolve("email").isEnabled()) {
            return MODE_EMAIL;
        }
        throw new cn.zest.sso.common.exception.SsoException(
                cn.zest.sso.common.exception.ErrorCode.BAD_REQUEST,
                "无可用的 step-up MFA 通道，请启用邮件或短信模块");
    }

    public MfaChannelAdapter resolveAdapter(String mode) {
        return switch (mode) {
            case MODE_EMAIL -> channelRegistry.resolveEnabled("email");
            case MODE_ALIYUN_SMS -> channelRegistry.resolveEnabled("aliyun-sms");
            case MODE_TENCENT_SMS -> channelRegistry.resolveEnabled("tencent-sms");
            default -> channelRegistry.resolve("totp");
        };
    }

    private String firstEnabledSmsKey(SsoUser user) {
        boolean phoneReady = StringUtils.hasText(user.getUsername())
                && user.getUsername().matches("^1\\d{10}$");
        if (!phoneReady) {
            return null;
        }
        return Arrays.stream(new String[]{"aliyun-sms", "tencent-sms"})
                .filter(key -> channelRegistry.resolve(key).isEnabled())
                .findFirst()
                .map(key -> "aliyun-sms".equals(key) ? MODE_ALIYUN_SMS : MODE_TENCENT_SMS)
                .orElse(null);
    }
}
