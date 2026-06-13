package cn.zest.sso.server.service;



import cn.zest.sso.common.constant.SsoConstants;

import cn.zest.sso.common.enums.AuditEventType;

import cn.zest.sso.common.enums.UserStatus;

import cn.zest.sso.common.exception.ErrorCode;

import cn.zest.sso.common.exception.SsoException;

import cn.zest.sso.server.domain.dto.CreateUserRequest;

import cn.zest.sso.server.domain.dto.UpdateUserRequest;

import cn.zest.sso.server.domain.entity.SsoRole;

import cn.zest.sso.server.domain.entity.SsoTenant;

import cn.zest.sso.server.domain.entity.SsoUser;

import cn.zest.sso.server.domain.entity.SsoUserRole;

import cn.zest.sso.server.domain.entity.SsoUserTenant;

import cn.zest.sso.server.domain.mapper.SsoRoleMapper;

import cn.zest.sso.server.domain.mapper.SsoTenantMapper;

import cn.zest.sso.server.domain.mapper.SsoUserMapper;

import cn.zest.sso.server.domain.mapper.SsoUserRoleMapper;

import cn.zest.sso.server.domain.mapper.SsoUserTenantMapper;

import cn.zest.sso.server.domain.vo.TenantVO;

import cn.zest.sso.server.domain.vo.UserInfoVO;

import cn.zest.sso.server.security.LoginAttemptService;

import cn.zest.sso.server.support.AdminAuditSupport;

import cn.zest.sso.server.support.AdminRbacSupport;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.CollectionUtils;



import java.time.LocalDateTime;

import java.util.List;



@Service

@RequiredArgsConstructor

public class UserService {



    private final SsoUserMapper userMapper;

    private final SsoRoleMapper roleMapper;

    private final SsoTenantMapper tenantMapper;

    private final SsoUserRoleMapper userRoleMapper;

    private final SsoUserTenantMapper userTenantMapper;

    private final PasswordEncoder passwordEncoder;

    private final LoginAttemptService loginAttemptService;

    private final AdminAuditSupport auditSupport;

    private final PasswordPolicyService passwordPolicyService;

    private final GroupService groupService;



    public UserInfoVO getUserInfo(Long userId) {

        SsoUser user = userMapper.selectById(userId);

        if (user == null) {

            throw new SsoException(ErrorCode.USER_NOT_FOUND, "用户不存在");

        }

        return toUserInfoVO(user);

    }



    public UserInfoVO getUserInfoByUsername(String username) {

        SsoUser user = userMapper.selectOne(new LambdaQueryWrapper<SsoUser>()

                .eq(SsoUser::getUsername, username));

        if (user == null) {

            throw new SsoException(ErrorCode.USER_NOT_FOUND, "用户不存在");

        }

        return toUserInfoVO(user);

    }



    public Page<UserInfoVO> pageUsers(int page, int size, String keyword) {

        LambdaQueryWrapper<SsoUser> wrapper = new LambdaQueryWrapper<>();

        if (keyword != null && !keyword.isBlank()) {

            wrapper.and(w -> w.like(SsoUser::getUsername, keyword)

                    .or().like(SsoUser::getEmail, keyword)

                    .or().like(SsoUser::getDisplayName, keyword));

        }

        wrapper.orderByDesc(SsoUser::getCreateTime);

        applyTenantScope(wrapper);

        Page<SsoUser> userPage = userMapper.selectPage(new Page<>(page, size), wrapper);

        Page<UserInfoVO> result = new Page<>(page, size, userPage.getTotal());

        result.setRecords(userPage.getRecords().stream().map(this::toUserInfoVO).toList());

        return result;

    }



    @Transactional(rollbackFor = Exception.class)

    public UserInfoVO createUser(CreateUserRequest request) {

        AdminRbacSupport.assertOperatorCanAssignRoles(auditSupport.isSsoAdmin(), request.getRoleCodes());
        AdminRbacSupport.assertTenantAdminCanAssignRoles(auditSupport.isTenantAdminOnly(), request.getRoleCodes());

        if (auditSupport.isTenantAdminOnly()) {
            Long tenantId = auditSupport.currentTenantId();
            if (tenantId == null) {
                throw new SsoException(ErrorCode.FORBIDDEN, "租户管理员未绑定租户");
            }
            request.setTenantIds(List.of(tenantId));
            request.setDefaultTenantId(tenantId);
        }

        Long count = userMapper.selectCount(new LambdaQueryWrapper<SsoUser>()

                .eq(SsoUser::getUsername, request.getUsername()));

        if (count > 0) {

            throw new SsoException(ErrorCode.CONFLICT, "用户名已存在");

        }



        SsoUser user = new SsoUser();

        user.setUsername(request.getUsername());

        user.setEmail(request.getEmail());

        passwordPolicyService.validateNewPassword(null, request.getPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername());

        user.setStatus(UserStatus.ACTIVE.getCode());

        user.setIsSuperAdmin(0);

        user.setPasswordChangedAt(LocalDateTime.now());

        userMapper.insert(user);



        bindRoles(user.getId(), request.getRoleCodes());

        bindTenants(user.getId(), request.getTenantIds(), request.getDefaultTenantId());

        groupService.bindUserGroups(user.getId(), request.getGroupIds());

        passwordPolicyService.recordPasswordChange(user.getId(), user.getPasswordHash());

        auditSupport.log(AuditEventType.USER_CREATE, user.getUsername(), user.getDisplayName());

        return toUserInfoVO(user);

    }



    public void updateLastLogin(Long userId, String ip) {

        SsoUser user = new SsoUser();

        user.setId(userId);

        user.setLastLoginAt(LocalDateTime.now());

        user.setLastLoginIp(ip);

        userMapper.updateById(user);

    }



    @Transactional(rollbackFor = Exception.class)

    public UserInfoVO updateUser(Long userId, UpdateUserRequest request) {

        SsoUser user = userMapper.selectById(userId);

        if (user == null) {

            throw new SsoException(ErrorCode.USER_NOT_FOUND, "用户不存在");

        }

        assertCanManageUser(userId);

        AdminRbacSupport.assertOperatorCanAssignRoles(auditSupport.isSsoAdmin(), request.getRoleCodes());



        if (request.getEmail() != null) {

            user.setEmail(request.getEmail());

        }

        if (request.getDisplayName() != null) {

            user.setDisplayName(request.getDisplayName());

        }

        if (request.getStatus() != null) {

            user.setStatus(request.getStatus());

        }

        userMapper.updateById(user);



        if (request.getRoleCodes() != null) {

            userRoleMapper.delete(new LambdaQueryWrapper<SsoUserRole>()

                    .eq(SsoUserRole::getUserId, userId));

            bindRoles(userId, request.getRoleCodes());

        }

        if (request.getTenantIds() != null) {

            userTenantMapper.delete(new LambdaQueryWrapper<SsoUserTenant>()

                    .eq(SsoUserTenant::getUserId, userId));

            bindTenants(userId, request.getTenantIds(), request.getDefaultTenantId());

        }

        if (request.getGroupIds() != null) {

            groupService.bindUserGroups(userId, request.getGroupIds());

        }



        auditSupport.log(AuditEventType.USER_UPDATE, user.getUsername(), user.getDisplayName());

        return toUserInfoVO(userMapper.selectById(userId));

    }



    @Transactional(rollbackFor = Exception.class)

    public void resetPassword(Long userId, String newPassword) {

        SsoUser user = userMapper.selectById(userId);

        if (user == null) {

            throw new SsoException(ErrorCode.USER_NOT_FOUND, "用户不存在");

        }

        assertCanManageUser(userId);

        passwordPolicyService.validateNewPassword(userId, newPassword);

        String encoded = passwordEncoder.encode(newPassword);

        SsoUser update = new SsoUser();

        update.setId(userId);

        update.setPasswordHash(encoded);

        update.setStatus(UserStatus.ACTIVE.getCode());

        update.setPasswordChangedAt(LocalDateTime.now());

        userMapper.updateById(update);

        passwordPolicyService.recordPasswordChange(userId, encoded);

        loginAttemptService.unlockAccount(user.getUsername());

        auditSupport.log(AuditEventType.PASSWORD_CHANGE, user.getUsername(), "管理员重置密码");

    }



    @Transactional(rollbackFor = Exception.class)

    public void changePassword(Long userId, String currentPassword, String newPassword) {

        SsoUser user = userMapper.selectById(userId);

        if (user == null) {

            throw new SsoException(ErrorCode.USER_NOT_FOUND, "用户不存在");

        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {

            throw new SsoException(ErrorCode.INVALID_CREDENTIALS, "当前密码不正确");

        }

        passwordPolicyService.validateNewPassword(userId, newPassword);

        String encoded = passwordEncoder.encode(newPassword);

        SsoUser update = new SsoUser();

        update.setId(userId);

        update.setPasswordHash(encoded);

        update.setPasswordChangedAt(LocalDateTime.now());

        userMapper.updateById(update);

        passwordPolicyService.recordPasswordChange(userId, encoded);

        auditSupport.log(AuditEventType.PASSWORD_CHANGE, user.getUsername(), "用户自助修改密码");

    }



    @Transactional(rollbackFor = Exception.class)

    public void disableUser(Long userId) {

        SsoUser user = requireUser(userId);

        assertCanManageUser(userId);

        SsoUser update = new SsoUser();

        update.setId(userId);

        update.setStatus(UserStatus.DISABLED.getCode());

        userMapper.updateById(update);

        auditSupport.log(AuditEventType.USER_DISABLE, user.getUsername(), user.getDisplayName());

    }



    @Transactional(rollbackFor = Exception.class)

    public void enableUser(Long userId) {

        SsoUser user = requireUser(userId);

        assertCanManageUser(userId);

        if (user.getStatus() == UserStatus.LOCKED.getCode()) {

            throw new SsoException(ErrorCode.BAD_REQUEST, "锁定用户请使用解锁操作");

        }

        SsoUser update = new SsoUser();

        update.setId(userId);

        update.setStatus(UserStatus.ACTIVE.getCode());

        userMapper.updateById(update);

        loginAttemptService.unlockAccount(user.getUsername());

        auditSupport.log(AuditEventType.USER_ENABLE, user.getUsername(), user.getDisplayName());

    }



    @Transactional(rollbackFor = Exception.class)

    public void unlockUser(Long userId) {

        SsoUser user = requireUser(userId);

        assertCanManageUser(userId);

        SsoUser update = new SsoUser();

        update.setId(userId);

        update.setStatus(UserStatus.ACTIVE.getCode());

        userMapper.updateById(update);

        loginAttemptService.unlockAccount(user.getUsername());

        auditSupport.log(AuditEventType.USER_UNLOCK, user.getUsername(), user.getDisplayName());

    }



    @Transactional(rollbackFor = Exception.class)

    public void deleteUser(Long userId) {

        SsoUser user = requireUser(userId);

        assertCanManageUser(userId);

        if ("admin".equals(user.getUsername())) {

            throw new SsoException(ErrorCode.FORBIDDEN, "默认管理员不可删除");

        }

        List<String> roles = roleMapper.selectByUserId(userId).stream().map(SsoRole::getCode).toList();

        if (roles.contains(SsoConstants.ROLE_SSO_ADMIN)) {

            long adminCount = countUsersWithRole(SsoConstants.ROLE_SSO_ADMIN);

            if (adminCount <= 1) {

                throw new SsoException(ErrorCode.FORBIDDEN, "不能删除最后一个 SSO 管理员");

            }

        }

        userRoleMapper.delete(new LambdaQueryWrapper<SsoUserRole>().eq(SsoUserRole::getUserId, userId));

        userTenantMapper.delete(new LambdaQueryWrapper<SsoUserTenant>().eq(SsoUserTenant::getUserId, userId));

        userMapper.deleteById(userId);

        auditSupport.log(AuditEventType.USER_DELETE, user.getUsername(), user.getDisplayName());

    }



    public boolean hasAdminRole(Long userId) {

        return roleMapper.selectByUserId(userId).stream()

                .anyMatch(role -> SsoConstants.ROLE_SSO_ADMIN.equals(role.getCode())
                        || SsoConstants.ROLE_SSO_OPERATOR.equals(role.getCode())
                        || SsoConstants.ROLE_TENANT_ADMIN.equals(role.getCode()));

    }



    private void assertCanManageUser(Long userId) {

        if (auditSupport.isSsoAdmin()) {

            return;

        }

        assertUserInTenantScope(userId);

        List<String> roles = roleMapper.selectByUserId(userId).stream().map(SsoRole::getCode).toList();

        AdminRbacSupport.assertOperatorCanManageUser(false, roles);

    }

    private void applyTenantScope(LambdaQueryWrapper<SsoUser> wrapper) {
        if (!auditSupport.isTenantAdminOnly()) {
            return;
        }
        Long tenantId = auditSupport.currentTenantId();
        if (tenantId == null) {
            wrapper.eq(SsoUser::getId, -1L);
            return;
        }
        List<Long> userIds = userTenantMapper.selectList(new LambdaQueryWrapper<SsoUserTenant>()
                        .eq(SsoUserTenant::getTenantId, tenantId))
                .stream().map(SsoUserTenant::getUserId).distinct().toList();
        if (userIds.isEmpty()) {
            wrapper.eq(SsoUser::getId, -1L);
        } else {
            wrapper.in(SsoUser::getId, userIds);
        }
    }

    private void assertUserInTenantScope(Long userId) {
        if (!auditSupport.isTenantAdminOnly()) {
            return;
        }
        Long tenantId = auditSupport.currentTenantId();
        if (tenantId == null) {
            throw new SsoException(ErrorCode.FORBIDDEN, "租户管理员未绑定租户");
        }
        Long count = userTenantMapper.selectCount(new LambdaQueryWrapper<SsoUserTenant>()
                .eq(SsoUserTenant::getUserId, userId)
                .eq(SsoUserTenant::getTenantId, tenantId));
        if (count == 0) {
            throw new SsoException(ErrorCode.FORBIDDEN, "无权管理其他租户用户");
        }
    }



    private long countUsersWithRole(String roleCode) {

        SsoRole role = roleMapper.selectOne(new LambdaQueryWrapper<SsoRole>().eq(SsoRole::getCode, roleCode));

        if (role == null) {

            return 0;

        }

        return userRoleMapper.selectCount(new LambdaQueryWrapper<SsoUserRole>().eq(SsoUserRole::getRoleId, role.getId()));

    }



    private SsoUser requireUser(Long userId) {

        SsoUser user = userMapper.selectById(userId);

        if (user == null) {

            throw new SsoException(ErrorCode.USER_NOT_FOUND, "用户不存在");

        }

        return user;

    }



    private void bindRoles(Long userId, List<String> roleCodes) {

        if (CollectionUtils.isEmpty(roleCodes)) {

            SsoRole defaultRole = roleMapper.selectOne(new LambdaQueryWrapper<SsoRole>()

                    .eq(SsoRole::getCode, SsoConstants.ROLE_USER));

            if (defaultRole != null) {

                insertUserRole(userId, defaultRole.getId());

            }

            return;

        }

        for (String code : roleCodes) {

            SsoRole role = roleMapper.selectOne(new LambdaQueryWrapper<SsoRole>()

                    .eq(SsoRole::getCode, code));

            if (role != null) {

                insertUserRole(userId, role.getId());

            }

        }

    }



    private void bindTenants(Long userId, List<Long> tenantIds, Long defaultTenantId) {

        if (CollectionUtils.isEmpty(tenantIds)) {

            SsoTenant defaultTenant = tenantMapper.selectOne(new LambdaQueryWrapper<SsoTenant>()

                    .eq(SsoTenant::getCode, "default"));

            if (defaultTenant != null) {

                insertUserTenant(userId, defaultTenant.getId(), 1);

            }

            return;

        }

        for (Long tenantId : tenantIds) {

            int isDefault = tenantId.equals(defaultTenantId) ? 1 : 0;

            insertUserTenant(userId, tenantId, isDefault);

        }

    }



    private void insertUserRole(Long userId, Long roleId) {

        SsoUserRole ur = new SsoUserRole();

        ur.setUserId(userId);

        ur.setRoleId(roleId);

        userRoleMapper.insert(ur);

    }



    private void insertUserTenant(Long userId, Long tenantId, int isDefault) {

        SsoUserTenant ut = new SsoUserTenant();

        ut.setUserId(userId);

        ut.setTenantId(tenantId);

        ut.setIsDefault(isDefault);

        userTenantMapper.insert(ut);

    }



    private UserInfoVO toUserInfoVO(SsoUser user) {

        List<String> roles = roleMapper.selectByUserId(user.getId()).stream()

                .map(SsoRole::getCode).toList();

        List<String> groupRoles = roleMapper.selectByUserGroups(user.getId()).stream()
                .map(SsoRole::getCode).toList();

        java.util.LinkedHashSet<String> mergedRoles = new java.util.LinkedHashSet<>(roles);
        mergedRoles.addAll(groupRoles);

        List<String> groups = groupService.listGroupCodesByUserId(user.getId());
        List<Long> groupIds = groupService.listGroupIdsByUserId(user.getId());

        List<SsoTenant> tenants = tenantMapper.selectByUserId(user.getId());

        SsoTenant defaultTenant = tenantMapper.selectDefaultByUserId(user.getId());



        List<TenantVO> tenantVOs = tenants.stream()

                .map(t -> TenantVO.builder()

                        .id(t.getId())

                        .code(t.getCode())

                        .name(t.getName())

                        .status(t.getStatus())

                        .isDefault(defaultTenant != null && t.getId().equals(defaultTenant.getId()))

                        .system("default".equals(t.getCode()))

                        .build())

                .toList();



        return UserInfoVO.builder()

                .id(user.getId())

                .username(user.getUsername())

                .email(user.getEmail())

                .displayName(user.getDisplayName())

                .status(user.getStatus())

                .superAdmin(user.getIsSuperAdmin() != null && user.getIsSuperAdmin() == 1)

                .roles(new java.util.ArrayList<>(mergedRoles))

                .groups(groups)

                .groupIds(groupIds)

                .tenants(tenantVOs)

                .defaultTenantId(defaultTenant != null ? defaultTenant.getId() : null)

                .lastLoginAt(user.getLastLoginAt())

                .lastLoginIp(user.getLastLoginIp())

                .mfaEnabled(user.getMfaEnabled() != null && user.getMfaEnabled() == 1)

                .build();

    }

}

