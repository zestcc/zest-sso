package cn.zest.sso.server.security;

import cn.zest.sso.common.enums.UserStatus;
import cn.zest.sso.server.domain.entity.SsoGroup;
import cn.zest.sso.server.domain.entity.SsoRole;
import cn.zest.sso.server.domain.entity.SsoTenant;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.mapper.SsoGroupMapper;
import cn.zest.sso.server.domain.mapper.SsoRoleMapper;
import cn.zest.sso.server.domain.mapper.SsoTenantMapper;
import cn.zest.sso.server.domain.mapper.SsoUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SsoUserDetailsService implements UserDetailsService {

    private final SsoUserMapper userMapper;
    private final SsoRoleMapper roleMapper;
    private final SsoGroupMapper groupMapper;
    private final SsoTenantMapper tenantMapper;
    private final LoginAttemptService loginAttemptService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        loginAttemptService.checkAccountLock(username);
        SsoUser user = userMapper.selectOne(new LambdaQueryWrapper<SsoUser>()
                .eq(SsoUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return buildUserDetails(user);
    }

    public SsoUserDetails loadByUserId(Long userId) {
        SsoUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }
        return buildUserDetails(user);
    }

    private SsoUserDetails buildUserDetails(SsoUser user) {
        Set<String> roleCodes = new LinkedHashSet<>();
        roleMapper.selectByUserId(user.getId()).stream().map(SsoRole::getCode).forEach(roleCodes::add);
        roleMapper.selectByUserGroups(user.getId()).stream().map(SsoRole::getCode).forEach(roleCodes::add);

        List<String> groups = groupMapper.selectByUserId(user.getId()).stream()
                .map(SsoGroup::getCode)
                .toList();

        SsoTenant defaultTenant = tenantMapper.selectDefaultByUserId(user.getId());
        Long tenantId = defaultTenant != null ? defaultTenant.getId() : null;
        String tenantCode = defaultTenant != null ? defaultTenant.getCode() : null;

        boolean enabled = UserStatus.ACTIVE.getCode() == user.getStatus();
        boolean accountNonLocked = UserStatus.LOCKED.getCode() != user.getStatus();
        boolean superAdmin = user.getIsSuperAdmin() != null && user.getIsSuperAdmin() == 1;

        return new SsoUserDetails(
                user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getEmail(), user.getDisplayName(),
                enabled, accountNonLocked, superAdmin, new ArrayList<>(roleCodes), groups,
                tenantId, tenantCode
        );
    }
}
