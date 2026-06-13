package cn.zest.sso.plugin.tencent.sms;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.plugin.PluginRuntimeContext;
import cn.zest.sso.plugin.mfa.MfaChannelAdapter;
import cn.zest.sso.plugin.mfa.MfaChannelDescriptor;
import cn.zest.sso.plugin.mfa.MfaUserContext;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class TencentSmsMfaChannelAdapter implements MfaChannelAdapter {

    private static final String CODE_PREFIX = "sso:mfa:sms-code:tencent:";

    private final PluginRuntimeContext pluginRuntimeContext;
    private final StringRedisTemplate redisTemplate;

    @Override
    public String channelKey() {
        return "tencent-sms";
    }

    @Override
    public String pluginName() {
        return "腾讯云短信";
    }

    @Override
    public Map<String, String> configFieldHints() {
        return Map.of(
                "secretId", "SecretId",
                "secretKey", "SecretKey",
                "sdkAppId", "SdkAppId",
                "signName", "签名",
                "templateId", "模板 ID",
                "region", "区域（默认 ap-guangzhou）"
        );
    }

    @Override
    public MfaChannelDescriptor descriptor() {
        Map<String, String> cfg = pluginRuntimeContext.getPluginConfig(channelKey());
        return new MfaChannelDescriptor(
                channelKey(), pluginName(), "登录 step-up 短信验证码",
                true, isEnabled(), credentialsReady(cfg), configFieldHints());
    }

    @Override
    public boolean isEnabled() {
        Map<String, String> cfg = pluginRuntimeContext.getPluginConfig(channelKey());
        return pluginRuntimeContext.isPluginEnabled(channelKey()) && credentialsReady(cfg);
    }

    @Override
    public String sendChallenge(MfaUserContext user, String challengeToken) {
        if (!isEnabled()) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "腾讯云短信 MFA 未启用或未配置");
        }
        String phone = resolvePhone(user);
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        redisTemplate.opsForValue().set(CODE_PREFIX + challengeToken, code, Duration.ofMinutes(5));
        sendSms(phone, code);
        return maskPhone(phone);
    }

    @Override
    public void verifyCode(String challengeToken, String code) {
        String expected = redisTemplate.opsForValue().get(CODE_PREFIX + challengeToken);
        if (expected == null || !expected.equals(code)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "短信验证码错误或已过期");
        }
    }

    private void sendSms(String phone, String code) {
        Map<String, String> cfg = pluginRuntimeContext.getPluginConfig(channelKey());
        try {
            Credential cred = new Credential(cfg.get("secretId"), cfg.get("secretKey"));
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("sms.tencentcloudapi.com");
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            String region = cfg.getOrDefault("region", "ap-guangzhou");
            SmsClient client = new SmsClient(cred, region, clientProfile);
            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(cfg.get("sdkAppId"));
            request.setSignName(cfg.get("signName"));
            request.setTemplateId(cfg.get("templateId"));
            request.setPhoneNumberSet(new String[]{"+86" + phone});
            request.setTemplateParamSet(new String[]{code});
            SendSmsResponse response = client.SendSms(request);
            if (response.getSendStatusSet() == null || response.getSendStatusSet().length == 0
                    || !"Ok".equalsIgnoreCase(response.getSendStatusSet()[0].getCode())) {
                String msg = response.getSendStatusSet() != null && response.getSendStatusSet().length > 0
                        ? response.getSendStatusSet()[0].getMessage() : "unknown";
                throw new SsoException(ErrorCode.INTERNAL_ERROR, "腾讯云短信发送失败: " + msg);
            }
        } catch (SsoException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("腾讯云短信发送异常", ex);
            throw new SsoException(ErrorCode.INTERNAL_ERROR, "腾讯云短信发送失败");
        }
    }

    private boolean credentialsReady(Map<String, String> cfg) {
        return StringUtils.hasText(cfg.get("secretId"))
                && StringUtils.hasText(cfg.get("secretKey"))
                && StringUtils.hasText(cfg.get("sdkAppId"))
                && StringUtils.hasText(cfg.get("templateId"))
                && StringUtils.hasText(cfg.get("signName"));
    }

    private String resolvePhone(MfaUserContext user) {
        if (StringUtils.hasText(user.username()) && user.username().matches("^1\\d{10}$")) {
            return user.username();
        }
        throw new SsoException(ErrorCode.BAD_REQUEST, "用户未配置手机号（username 需为 11 位手机号）");
    }

    private String maskPhone(String phone) {
        if (phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
