package cn.zest.sso.server.mfa.channel;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.mfa.spi.MfaChannelAdapter;
import cn.zest.sso.server.mfa.spi.MfaChannelDescriptor;
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
    public MfaChannelDescriptor descriptor() {
        return new MfaChannelDescriptor(
                channelKey(), "邮件 OTP", "自适应登录风险时发送邮件验证码",
                isEnabled(), true,
                Map.of("mail.enabled", "需启用 zest.sso.mail.enabled"));
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getMfa().getChannels().getEmail().isEnabled();
    }

    @Override
    public String sendChallenge(SsoUser user, String challengeToken) {
        if (!StringUtils.hasText(user.getEmail())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "用户未配置邮箱，无法发送邮件 OTP");
        }
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        redisTemplate.opsForValue().set(CODE_PREFIX + challengeToken, code, Duration.ofMinutes(5));
        emailService.sendLoginOtp(user.getEmail(), code, 5);
        return maskEmail(user.getEmail());
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
