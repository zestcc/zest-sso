package cn.zest.sso.server.service;

import cn.zest.sso.common.constant.SsoConstants;
import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.mapper.SsoUserMapper;
import cn.zest.sso.server.support.AdminAuditSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cn.zest.sso.server.metrics.SsoMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

/**
 * 自助密码重置（对标 Okta/Keycloak forgot password）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final SsoUserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailService emailService;
    private final SsoProperties ssoProperties;
    private final AdminAuditSupport auditSupport;
    private final SsoMetrics ssoMetrics;

    public void requestReset(String usernameOrEmail) {
        if (!StringUtils.hasText(usernameOrEmail)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "用户名或邮箱不能为空");
        }
        SsoUser user = findUser(usernameOrEmail.trim());
        if (user == null) {
            // 防止用户枚举，统一返回成功语义
            log.info("密码重置请求：用户不存在 {}", usernameOrEmail);
            return;
        }
        String token = generateToken();
        String key = SsoConstants.REDIS_PASSWORD_RESET_PREFIX + token;
        int ttlMinutes = ssoProperties.getPasswordReset().getTokenTtlMinutes();
        redisTemplate.opsForValue().set(key, String.valueOf(user.getId()),
                Duration.ofMinutes(ttlMinutes));

        String resetUrl = ssoProperties.getPasswordReset().getPublicBaseUrl().replaceAll("/$", "")
                + "/reset-password?token=" + token;
        emailService.sendPasswordReset(user.getEmail(), resetUrl, ttlMinutes);
        auditSupport.log(AuditEventType.PASSWORD_RESET_REQUEST, user.getUsername(),
                "已发送密码重置链接");
        ssoMetrics.recordPasswordResetRequest();
        log.info("密码重置 token 已生成（用户 {}），开发环境链接: {}", user.getUsername(), resetUrl);
    }

    public void completeReset(String token, String newPassword) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(newPassword)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "token 与新密码不能为空");
        }
        String key = SsoConstants.REDIS_PASSWORD_RESET_PREFIX + token;
        String userIdStr = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(userIdStr)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "重置链接无效或已过期");
        }
        Long userId = Long.parseLong(userIdStr);
        SsoUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new SsoException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        passwordPolicyService.validateNewPassword(userId, newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        passwordPolicyService.recordPasswordChange(userId, user.getPasswordHash());
        redisTemplate.delete(key);
        auditSupport.log(AuditEventType.PASSWORD_RESET_COMPLETE, user.getUsername(), "自助重置密码");
    }

    private SsoUser findUser(String usernameOrEmail) {
        SsoUser byName = userMapper.selectOne(new LambdaQueryWrapper<SsoUser>()
                .eq(SsoUser::getUsername, usernameOrEmail));
        if (byName != null) {
            return byName;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<SsoUser>()
                .eq(SsoUser::getEmail, usernameOrEmail));
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
