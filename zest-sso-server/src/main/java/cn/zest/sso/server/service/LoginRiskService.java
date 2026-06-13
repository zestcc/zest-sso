package cn.zest.sso.server.service;

import cn.zest.sso.server.config.SsoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 自适应 MFA：新 IP 触发 step-up（对标 Okta ThreatInsight 轻量版）。
 */
@Service
@RequiredArgsConstructor
public class LoginRiskService {

    private static final String KNOWN_IP_PREFIX = "sso:risk:known-ip:";
    private static final String STEP_UP_PREFIX = "sso:risk:step-up:";

    private final StringRedisTemplate redisTemplate;
    private final SsoProperties ssoProperties;

    public boolean requiresStepUp(String username, String ip) {
        if (!ssoProperties.getRisk().isStepUpOnNewIp() || !StringUtils.hasText(username) || !StringUtils.hasText(ip)) {
            return false;
        }
        if ("unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        String key = KNOWN_IP_PREFIX + username;
        Boolean known = redisTemplate.opsForSet().isMember(key, ip);
        return known == null || !known;
    }

    public void recordSuccessfulLogin(String username, String ip) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            return;
        }
        String key = KNOWN_IP_PREFIX + username;
        redisTemplate.opsForSet().add(key, ip);
        redisTemplate.expire(key, Duration.ofDays(ssoProperties.getRisk().getKnownIpTtlDays()));
        redisTemplate.delete(STEP_UP_PREFIX + username);
    }

    public void markStepUpPending(String username) {
        redisTemplate.opsForValue().set(STEP_UP_PREFIX + username, "1", Duration.ofMinutes(10));
    }
}
