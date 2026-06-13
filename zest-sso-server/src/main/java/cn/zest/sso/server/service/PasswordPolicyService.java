package cn.zest.sso.server.service;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.entity.SsoPasswordHistory;
import cn.zest.sso.server.domain.entity.SsoPasswordPolicy;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.mapper.SsoPasswordHistoryMapper;
import cn.zest.sso.server.domain.mapper.SsoPasswordPolicyMapper;
import cn.zest.sso.server.domain.vo.PasswordPolicyVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    private final SsoPasswordPolicyMapper policyMapper;
    private final SsoPasswordHistoryMapper historyMapper;
    private final PasswordEncoder passwordEncoder;

    public PasswordPolicyVO getPolicy() {
        return toVO(requirePolicy());
    }

    @Transactional(rollbackFor = Exception.class)
    public PasswordPolicyVO updatePolicy(PasswordPolicyVO request) {
        SsoPasswordPolicy policy = requirePolicy();
        policy.setMinLength(request.getMinLength());
        policy.setRequireUppercase(boolToInt(request.isRequireUppercase()));
        policy.setRequireLowercase(boolToInt(request.isRequireLowercase()));
        policy.setRequireDigit(boolToInt(request.isRequireDigit()));
        policy.setRequireSpecial(boolToInt(request.isRequireSpecial()));
        policy.setPasswordHistoryCount(request.getPasswordHistoryCount());
        policy.setMaxAgeDays(request.getMaxAgeDays());
        policyMapper.updateById(policy);
        return toVO(policy);
    }

    public void validateNewPassword(Long userId, String rawPassword) {
        SsoPasswordPolicy policy = requirePolicy();
        if (rawPassword == null || rawPassword.length() < policy.getMinLength()) {
            throw new SsoException(ErrorCode.BAD_REQUEST,
                    "密码长度至少 " + policy.getMinLength() + " 位");
        }
        if (isRequired(policy.getRequireUppercase()) && !UPPER.matcher(rawPassword).find()) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "密码须包含大写字母");
        }
        if (isRequired(policy.getRequireLowercase()) && !LOWER.matcher(rawPassword).find()) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "密码须包含小写字母");
        }
        if (isRequired(policy.getRequireDigit()) && !DIGIT.matcher(rawPassword).find()) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "密码须包含数字");
        }
        if (isRequired(policy.getRequireSpecial()) && !SPECIAL.matcher(rawPassword).find()) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "密码须包含特殊字符");
        }
        if (userId != null && policy.getPasswordHistoryCount() != null && policy.getPasswordHistoryCount() > 0) {
            List<SsoPasswordHistory> history = historyMapper.selectList(new LambdaQueryWrapper<SsoPasswordHistory>()
                    .eq(SsoPasswordHistory::getUserId, userId)
                    .orderByDesc(SsoPasswordHistory::getCreateTime)
                    .last("LIMIT " + policy.getPasswordHistoryCount()));
            for (SsoPasswordHistory item : history) {
                if (passwordEncoder.matches(rawPassword, item.getPasswordHash())) {
                    throw new SsoException(ErrorCode.BAD_REQUEST, "不能与最近使用过的密码相同");
                }
            }
        }
    }

    public boolean isPasswordExpired(SsoUser user) {
        SsoPasswordPolicy policy = requirePolicy();
        if (policy.getMaxAgeDays() == null || policy.getMaxAgeDays() <= 0) {
            return false;
        }
        if (user.getPasswordChangedAt() == null) {
            return true;
        }
        return user.getPasswordChangedAt().plusDays(policy.getMaxAgeDays()).isBefore(LocalDateTime.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordPasswordChange(Long userId, String encodedPassword) {
        SsoPasswordHistory history = new SsoPasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(encodedPassword);
        historyMapper.insert(history);

        SsoPasswordPolicy policy = requirePolicy();
        if (policy.getPasswordHistoryCount() != null && policy.getPasswordHistoryCount() > 0) {
            List<SsoPasswordHistory> all = historyMapper.selectList(new LambdaQueryWrapper<SsoPasswordHistory>()
                    .eq(SsoPasswordHistory::getUserId, userId)
                    .orderByDesc(SsoPasswordHistory::getCreateTime));
            for (int i = policy.getPasswordHistoryCount(); i < all.size(); i++) {
                historyMapper.deleteById(all.get(i).getId());
            }
        }
    }

    private SsoPasswordPolicy requirePolicy() {
        SsoPasswordPolicy policy = policyMapper.selectOne(new LambdaQueryWrapper<SsoPasswordPolicy>()
                .orderByAsc(SsoPasswordPolicy::getId)
                .last("LIMIT 1"));
        if (policy == null) {
            throw new SsoException(ErrorCode.INTERNAL_ERROR, "密码策略未初始化");
        }
        return policy;
    }

    private boolean isRequired(Integer flag) {
        return flag != null && flag == 1;
    }

    private int boolToInt(boolean value) {
        return value ? 1 : 0;
    }

    private PasswordPolicyVO toVO(SsoPasswordPolicy policy) {
        return PasswordPolicyVO.builder()
                .minLength(policy.getMinLength())
                .requireUppercase(isRequired(policy.getRequireUppercase()))
                .requireLowercase(isRequired(policy.getRequireLowercase()))
                .requireDigit(isRequired(policy.getRequireDigit()))
                .requireSpecial(isRequired(policy.getRequireSpecial()))
                .passwordHistoryCount(policy.getPasswordHistoryCount())
                .maxAgeDays(policy.getMaxAgeDays())
                .build();
    }
}
