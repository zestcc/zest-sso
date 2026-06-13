package cn.zest.sso.server.mfa.channel;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.plugin.mfa.MfaChannelAdapter;
import cn.zest.sso.plugin.mfa.MfaChannelDescriptor;
import cn.zest.sso.plugin.mfa.MfaUserContext;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmailMfaChannelAdapter implements MfaChannelAdapter {

    private static final String CODE_PREFIX = "sso:mfa:email-code:";

    private final SsoProperties ssoProperties;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public String channelKey() {
        return "email";
    }

    @Override
    public String pluginName() {
        return "邮件 OTP";
    }

    @Override
    public Map<String, String> configFieldHints() {
        return Map.of("mail.enabled", "需启用 zest.sso.mail.enabled");
    }

    @Override
    public MfaChannelDescriptor descriptor() {
        return new MfaChannelDescriptor(
                channelKey(), pluginName(), "自适应登录风险时发送邮件验证码",
                true, isEnabled(), ssoProperties.getMail().isEnabled(), configFieldHints());
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getMfa().getChannels().getEmail().isEnabled()
                && ssoProperties.getMail().isEnabled();
    }

    @Override
    public String sendChallenge(MfaUserContext user, String challengeToken) {
        if (!StringUtils.hasText(user.email())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "用户未配置邮箱，无法发送邮件 OTP");
        }
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        redisTemplate.opsForValue().set(CODE_PREFIX + challengeToken, code, Duration.ofMinutes(5));
        emailService.sendLoginOtp(user.email(), code, 5);
        return maskEmail(user.email());
    }

    @Override
    public void verifyCode(String challengeToken, String code) {
        String expected = redisTemplate.opsForValue().get(CODE_PREFIX + challengeToken);
        if (expected == null || !expected.equals(code)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "邮件验证码错误或已过期");
        }
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(at);
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
