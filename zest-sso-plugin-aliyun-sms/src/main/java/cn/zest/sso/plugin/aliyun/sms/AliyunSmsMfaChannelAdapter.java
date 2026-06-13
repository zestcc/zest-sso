package cn.zest.sso.plugin.aliyun.sms;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.plugin.PluginRuntimeContext;
import cn.zest.sso.plugin.mfa.MfaChannelAdapter;
import cn.zest.sso.plugin.mfa.MfaChannelDescriptor;
import cn.zest.sso.plugin.mfa.MfaUserContext;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class AliyunSmsMfaChannelAdapter implements MfaChannelAdapter {

    private static final String CODE_PREFIX = "sso:mfa:sms-code:aliyun:";

    private final PluginRuntimeContext pluginRuntimeContext;
    private final StringRedisTemplate redisTemplate;

    @Override
    public String channelKey() {
        return "aliyun-sms";
    }

    @Override
    public String pluginName() {
        return "阿里云短信";
    }

    @Override
    public Map<String, String> configFieldHints() {
        return Map.of(
                "accessKeyId", "AccessKey ID",
                "accessKeySecret", "AccessKey Secret",
                "signName", "短信签名",
                "templateCode", "模板编号",
                "regionId", "区域（默认 cn-hangzhou）"
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
            throw new SsoException(ErrorCode.BAD_REQUEST, "阿里云短信 MFA 未启用或未配置");
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
            Config config = new Config()
                    .setAccessKeyId(cfg.get("accessKeyId"))
                    .setAccessKeySecret(cfg.get("accessKeySecret"))
                    .setEndpoint("dysmsapi.aliyuncs.com");
            String regionId = cfg.getOrDefault("regionId", "cn-hangzhou");
            config.setRegionId(regionId);
            Client client = new Client(config);
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(cfg.get("signName"))
                    .setTemplateCode(cfg.get("templateCode"))
                    .setTemplateParam("{\"code\":\"" + code + "\"}");
            SendSmsResponse response = client.sendSms(request);
            if (response.getBody() == null || !"OK".equalsIgnoreCase(response.getBody().getCode())) {
                String msg = response.getBody() != null ? response.getBody().getMessage() : "unknown";
                throw new SsoException(ErrorCode.INTERNAL_ERROR, "阿里云短信发送失败: " + msg);
            }
        } catch (SsoException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("阿里云短信发送异常", ex);
            throw new SsoException(ErrorCode.INTERNAL_ERROR, "阿里云短信发送失败");
        }
    }

    private boolean credentialsReady(Map<String, String> cfg) {
        return StringUtils.hasText(cfg.get("accessKeyId"))
                && StringUtils.hasText(cfg.get("accessKeySecret"))
                && StringUtils.hasText(cfg.get("signName"))
                && StringUtils.hasText(cfg.get("templateCode"));
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
