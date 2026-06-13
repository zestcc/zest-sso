package cn.zest.sso.server.security;

import cn.zest.sso.common.constant.SsoConstants;
import cn.zest.sso.common.enums.UserStatus;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.mapper.SsoUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录失败计数与账号锁定。
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String USER_FAIL_PREFIX = "sso:login:user-fail:";

    private final StringRedisTemplate redisTemplate;
    private final SsoProperties ssoProperties;
    private final SsoUserMapper userMapper;

    public void checkAccountLock(String username) {
        SsoUser user = userMapper.selectOne(new LambdaQueryWrapper<SsoUser>()
                .eq(SsoUser::getUsername, username));
        if (user != null && UserStatus.LOCKED.getCode() == user.getStatus()) {
            throw new org.springframework.security.authentication.LockedException("账号已锁定，请联系管理员");
        }
        String key = USER_FAIL_PREFIX + username;
        String countStr = redisTemplate.opsForValue().get(key);
        if (countStr != null && Integer.parseInt(countStr) >= SsoConstants.MAX_LOGIN_ATTEMPTS) {
            lockUser(user);
            throw new org.springframework.security.authentication.LockedException("登录失败次数过多，账号已锁定");
        }
    }

    public void onLoginSuccess(String username) {
        redisTemplate.delete(USER_FAIL_PREFIX + username);
    }

    public void unlockAccount(String username) {
        redisTemplate.delete(USER_FAIL_PREFIX + username);
    }

    public void onLoginFailure(String username) {
        String key = USER_FAIL_PREFIX + username;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, SsoConstants.LOGIN_LOCK_MINUTES, TimeUnit.MINUTES);
        }
        if (count != null && count >= SsoConstants.MAX_LOGIN_ATTEMPTS) {
            SsoUser user = userMapper.selectOne(new LambdaQueryWrapper<SsoUser>()
                    .eq(SsoUser::getUsername, username));
            lockUser(user);
        }
    }

    private void lockUser(SsoUser user) {
        if (user == null) {
            return;
        }
        SsoUser update = new SsoUser();
        update.setId(user.getId());
        update.setStatus(UserStatus.LOCKED.getCode());
        userMapper.updateById(update);
    }
}
