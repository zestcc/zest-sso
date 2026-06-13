package cn.zest.sso.server.mfa.spi;

import cn.zest.sso.server.domain.entity.SsoUser;

/**
 * MFA 通道插拔适配器（TOTP / 邮件 / 短信等）。
 */
public interface MfaChannelAdapter {

    String channelKey();

    MfaChannelDescriptor descriptor();

    boolean isEnabled();

    /** 发送一次性验证码，返回展示给用户的提示（如手机号掩码） */
    String sendChallenge(SsoUser user, String challengeToken);

    void verifyCode(String challengeToken, String code);
}
