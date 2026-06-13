package cn.zest.sso.server.mfa.channel;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.mfa.spi.MfaChannelAdapter;
import cn.zest.sso.server.mfa.spi.MfaChannelDescriptor;
import cn.zest.sso.server.security.TotpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TotpMfaChannelAdapter implements MfaChannelAdapter {

    private final SsoProperties ssoProperties;

    @Override
    public String channelKey() {
        return "totp";
    }

    @Override
    public MfaChannelDescriptor descriptor() {
        return new MfaChannelDescriptor(
                channelKey(), "TOTP 验证器", "Google Authenticator / 微软验证器等",
                isEnabled(), true, Map.of());
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getMfa().getChannels().getTotp().isEnabled();
    }

    @Override
    public String sendChallenge(SsoUser user, String challengeToken) {
        throw new SsoException(ErrorCode.BAD_REQUEST, "TOTP 无需发送验证码");
    }

    @Override
    public void verifyCode(String challengeToken, String code) {
        throw new SsoException(ErrorCode.BAD_REQUEST, "TOTP 校验由 MfaService 内置处理");
    }

    public void verifySecret(String secret, String code) {
        if (!TotpUtil.verify(secret, code, 1)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "验证码错误");
        }
    }
}
