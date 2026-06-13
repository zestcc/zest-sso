package cn.zest.sso.server.mfa.channel;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.plugin.mfa.MfaChannelAdapter;
import cn.zest.sso.plugin.mfa.MfaChannelDescriptor;
import cn.zest.sso.plugin.mfa.MfaUserContext;
import cn.zest.sso.server.config.SsoProperties;
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
    public String pluginName() {
        return "TOTP 验证器";
    }

    @Override
    public Map<String, String> configFieldHints() {
        return Map.of();
    }

    @Override
    public MfaChannelDescriptor descriptor() {
        return new MfaChannelDescriptor(
                channelKey(), pluginName(), "Google Authenticator / 微软验证器等",
                true, isEnabled(), true, configFieldHints());
    }

    @Override
    public boolean isEnabled() {
        return ssoProperties.getMfa().getChannels().getTotp().isEnabled();
    }

    @Override
    public String sendChallenge(MfaUserContext user, String challengeToken) {
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
