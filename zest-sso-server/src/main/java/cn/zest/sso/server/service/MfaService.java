package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.mapper.SsoUserMapper;
import cn.zest.sso.server.domain.vo.LoginResultVO;
import cn.zest.sso.server.domain.vo.MfaSetupVO;
import cn.zest.sso.server.domain.vo.UserInfoVO;
import cn.zest.sso.server.mfa.MfaStepUpChannelSelector;
import cn.zest.sso.plugin.mfa.MfaChannelAdapter;
import cn.zest.sso.server.security.TotpUtil;
import cn.zest.sso.server.support.AdminAuditSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MfaService {

    public static final String MFA_MODE_TOTP = "TOTP";
    public static final String MFA_MODE_EMAIL = "EMAIL";

    private static final String MFA_PENDING_PREFIX = "sso:mfa:pending:";
    private static final String MFA_MODE_PREFIX = "sso:mfa:mode:";
    private static final String MFA_EMAIL_CODE_PREFIX = "sso:mfa:email-code:";
    private static final String MFA_SETUP_PREFIX = "sso:mfa:setup:";
    private static final String MFA_REDIRECT_PREFIX = "sso:mfa:redirect:";

    private final SsoUserMapper userMapper;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;
    private final AdminAuditSupport auditSupport;
    private final LoginRiskService loginRiskService;
    private final MfaStepUpChannelSelector stepUpChannelSelector;

    public LoginResultVO buildLoginResult(Long userId) {
        return buildLoginResult(userId, null);
    }

    public LoginResultVO buildLoginResult(Long userId, String clientIp) {
        SsoUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new SsoException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        boolean mfaEnabled = user.getMfaEnabled() != null && user.getMfaEnabled() == 1;
        boolean stepUp = loginRiskService.requiresStepUp(user.getUsername(), clientIp);
        if (!mfaEnabled && !stepUp) {
            if (StringUtils.hasText(clientIp)) {
                loginRiskService.recordSuccessfulLogin(user.getUsername(), clientIp);
            }
            return LoginResultVO.builder()
                    .mfaRequired(false)
                    .user(userService.getUserInfo(userId))
                    .build();
        }
        String mfaToken = UUID.randomUUID().toString().replace("-", "");
        String mode = stepUpChannelSelector.resolveBoundMode(user);
        redisTemplate.opsForValue().set(MFA_PENDING_PREFIX + mfaToken, String.valueOf(userId),
                Duration.ofMinutes(5));
        redisTemplate.opsForValue().set(MFA_MODE_PREFIX + mfaToken, mode, Duration.ofMinutes(5));
        String hint = null;
        if (!MfaStepUpChannelSelector.MODE_TOTP.equals(mode)) {
            MfaChannelAdapter adapter = stepUpChannelSelector.resolveAdapter(mode);
            hint = adapter.sendChallenge(MfaStepUpChannelSelector.toUserContext(user), mfaToken);
            loginRiskService.markStepUpPending(user.getUsername());
        }
        return LoginResultVO.builder()
                .mfaRequired(true)
                .mfaToken(mfaToken)
                .mfaMode(mode)
                .mfaHint(hint)
                .build();
    }

    public UserInfoVO verifyLogin(String mfaToken, String code) {
        Long userId = consumePendingToken(mfaToken);
        SsoUser user = requireUser(userId);
        verifyChallenge(mfaToken, user, code);
        redisTemplate.delete(MFA_PENDING_PREFIX + mfaToken);
        redisTemplate.delete(MFA_MODE_PREFIX + mfaToken);
        redisTemplate.delete(MFA_EMAIL_CODE_PREFIX + mfaToken);
        loginRiskService.recordSuccessfulLogin(user.getUsername(), user.getLastLoginIp());
        return userService.getUserInfo(userId);
    }

    public MfaSetupVO getSetupInfo(Long userId) {
        SsoUser user = requireUser(userId);
        String secret = redisTemplate.opsForValue().get(MFA_SETUP_PREFIX + userId);
        if (secret == null) {
            secret = TotpUtil.generateSecret();
            redisTemplate.opsForValue().set(MFA_SETUP_PREFIX + userId, secret, Duration.ofMinutes(10));
        }
        return MfaSetupVO.builder()
                .secret(secret)
                .otpAuthUrl(TotpUtil.buildOtpAuthUrl("ZestSSO", user.getUsername(), secret))
                .enabled(user.getMfaEnabled() != null && user.getMfaEnabled() == 1)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void enable(Long userId, String code) {
        SsoUser user = requireUser(userId);
        String secret = redisTemplate.opsForValue().get(MFA_SETUP_PREFIX + userId);
        if (secret == null) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "请先获取 MFA 绑定信息");
        }
        assertMfaCode(secret, code);
        SsoUser update = new SsoUser();
        update.setId(userId);
        update.setMfaSecret(secret);
        update.setMfaEnabled(1);
        userMapper.updateById(update);
        redisTemplate.delete(MFA_SETUP_PREFIX + userId);
        auditSupport.log(AuditEventType.MFA_ENABLE, user.getUsername(), "启用 MFA");
    }

    public void attachRedirectUrl(String mfaToken, String redirectUrl) {
        redisTemplate.opsForValue().set(MFA_REDIRECT_PREFIX + mfaToken, redirectUrl,
                Duration.ofMinutes(5));
    }

    public WebLoginResult verifyWebLogin(String mfaToken, String code) {
        Long userId = consumePendingToken(mfaToken);
        SsoUser user = requireUser(userId);
        String mode = redisTemplate.opsForValue().get(MFA_MODE_PREFIX + mfaToken);
        verifyChallenge(mfaToken, user, code);
        String redirect = redisTemplate.opsForValue().get(MFA_REDIRECT_PREFIX + mfaToken);
        if (redirect == null || redirect.isBlank()) {
            redirect = "/";
        }
        redisTemplate.delete(MFA_PENDING_PREFIX + mfaToken);
        redisTemplate.delete(MFA_MODE_PREFIX + mfaToken);
        redisTemplate.delete(MFA_EMAIL_CODE_PREFIX + mfaToken);
        redisTemplate.delete(MFA_REDIRECT_PREFIX + mfaToken);
        loginRiskService.recordSuccessfulLogin(user.getUsername(), user.getLastLoginIp());
        return new WebLoginResult(userId, redirect);
    }

    @Transactional(rollbackFor = Exception.class)
    public void adminReset(Long userId) {
        SsoUser user = requireUser(userId);
        SsoUser update = new SsoUser();
        update.setId(userId);
        update.setMfaSecret(null);
        update.setMfaEnabled(0);
        userMapper.updateById(update);
        auditSupport.log(AuditEventType.MFA_ADMIN_RESET, user.getUsername(), "管理员重置 MFA");
    }

    public record WebLoginResult(Long userId, String redirectUrl) {}

    @Transactional(rollbackFor = Exception.class)
    public void disable(Long userId, String code) {
        SsoUser user = requireUser(userId);
        if (user.getMfaSecret() == null) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "MFA 未启用");
        }
        assertMfaCode(user.getMfaSecret(), code);
        SsoUser update = new SsoUser();
        update.setId(userId);
        update.setMfaSecret(null);
        update.setMfaEnabled(0);
        userMapper.updateById(update);
        auditSupport.log(AuditEventType.MFA_DISABLE, user.getUsername(), "禁用 MFA");
    }

    private void verifyChallenge(String mfaToken, SsoUser user, String code) {
        String mode = redisTemplate.opsForValue().get(MFA_MODE_PREFIX + mfaToken);
        if (mode == null) {
            mode = MfaStepUpChannelSelector.MODE_TOTP;
        }
        if (MfaStepUpChannelSelector.MODE_TOTP.equals(mode)) {
            assertMfaCode(user.getMfaSecret(), code);
        } else {
            stepUpChannelSelector.resolveAdapter(mode).verifyCode(mfaToken, code);
        }
    }

    private Long consumePendingToken(String mfaToken) {
        String userId = redisTemplate.opsForValue().get(MFA_PENDING_PREFIX + mfaToken);
        if (userId == null) {
            throw new SsoException(ErrorCode.UNAUTHORIZED, "MFA 会话已过期，请重新登录");
        }
        return Long.parseLong(userId);
    }

    private void assertMfaCode(String secret, String code) {
        if (!TotpUtil.verify(secret, code, 1)) {
            throw new SsoException(ErrorCode.INVALID_CREDENTIALS, "验证码无效");
        }
    }

    private SsoUser requireUser(Long userId) {
        SsoUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new SsoException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
