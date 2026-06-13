package cn.zest.sso.server.mfa.channel;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.mfa.spi.MfaChannelAdapter;
import cn.zest.sso.server.mfa.spi.MfaChannelDescriptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunSmsMfaChannelAdapter implements MfaChannelAdapter {

    private static final String CODE_PREFIX = "sso:mfa:sms-code:aliyun:";

    private final SsoProperties ssoProperties;
    private final StringRedisTemplate redisTemplate;

    @Override
    public String channelKey() {
        return "aliyun-sms";
    }

    @Override
    public MfaChannelDescriptor descriptor() {
        SsoProperties.Mfa.AliyunSmsConfig cfg = ssoProperties.getMfa().getChannels().getAliyunSms();
        return new MfaChannelDescriptor(
                channelKey(), "阿里云短信", "登录 step-up 短信验证码（可选）",
                isEnabled(), credentialsReady(cfg),
                Map.of(
                        "accessKeyId", "阿里云 AccessKey",
                        "signName", "短信签名",
                        "templateCode", "模板编号"
                ));
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getModules().isSmsMfa()
                && ssoProperties.getMfa().getChannels().getAliyunSms().isEnabled()
                && credentialsReady(ssoProperties.getMfa().getChannels().getAliyunSms());
    }

    @Override
    public String sendChallenge(SsoUser user, String challengeToken) {
        if (!isEnabled()) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "阿里云短信 MFA 未启用");
        }
        String phone = resolvePhone(user);
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        redisTemplate.opsForValue().set(CODE_PREFIX + challengeToken, code, Duration.ofMinutes(5));
        // 生产可替换为阿里云 SDK；未引入依赖时记录日志便于开发联调
        log.info("[AliyunSMS-MFA] to={} code={} (配置 SDK 后改为真实发送)", maskPhone(phone), code);
        return maskPhone(phone);
    }

    @Override
    public void verifyCode(String challengeToken, String code) {
        String expected = redisTemplate.opsForValue().get(CODE_PREFIX + challengeToken);
        if (expected == null || !expected.equals(code)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "短信验证码错误或已过期");
        }
    }

    private boolean credentialsReady(SsoProperties.Mfa.AliyunSmsConfig cfg) {
        return StringUtils.hasText(cfg.getAccessKeyId())
                && StringUtils.hasText(cfg.getAccessKeySecret())
                && StringUtils.hasText(cfg.getSignName())
                && StringUtils.hasText(cfg.getTemplateCode());
    }

    private String resolvePhone(SsoUser user) {
        if (StringUtils.hasText(user.getUsername()) && user.getUsername().matches("^1\\d{10}$")) {
            return user.getUsername();
        }
        throw new SsoException(ErrorCode.BAD_REQUEST, "用户未配置手机号（username 需为 11 位手机号），无法发送短信 OTP");
    }

    private String maskPhone(String phone) {
        if (phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
