package cn.zest.sso.plugin.mfa;

import cn.zest.sso.plugin.PluggablePlugin;

public interface MfaChannelAdapter extends PluggablePlugin {

    @Override
    default String category() {
        return "mfa";
    }

    /** 与 {@link #pluginKey()} 一致 */
    String channelKey();

    @Override
    default String pluginKey() {
        return channelKey();
    }

    MfaChannelDescriptor descriptor();

    boolean isEnabled();

    String sendChallenge(MfaUserContext user, String challengeToken);

    void verifyCode(String challengeToken, String code);
}
