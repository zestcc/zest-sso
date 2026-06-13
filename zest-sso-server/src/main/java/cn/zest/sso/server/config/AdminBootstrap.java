package cn.zest.sso.server.config;



import cn.zest.sso.common.constant.SsoConstants;

import cn.zest.sso.common.enums.UserStatus;

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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;

import org.springframework.boot.ApplicationRunner;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Component;



@Slf4j

@Component

@RequiredArgsConstructor

public class AdminBootstrap implements ApplicationRunner {



    private static final String DEFAULT_ADMIN = "admin";

    private static final String DEFAULT_OPERATOR = "operator";

    private static final String DEFAULT_PASSWORD = "admin123";

    private static final String OPERATOR_PASSWORD = "operator123";



    private final SsoProperties ssoProperties;

    private final SsoUserMapper userMapper;

    private final SsoRoleMapper roleMapper;

    private final SsoTenantMapper tenantMapper;

    private final SsoUserRoleMapper userRoleMapper;

    private final SsoUserTenantMapper userTenantMapper;

    private final PasswordEncoder passwordEncoder;



    @Override

    public void run(ApplicationArguments args) {

        ensureAdminUser();

        ensureOperatorUser();

    }



    private void ensureAdminUser() {

        SsoUser admin = userMapper.selectOne(new LambdaQueryWrapper<SsoUser>()

                .eq(SsoUser::getUsername, DEFAULT_ADMIN));

        if (admin == null) {

            createUser(DEFAULT_ADMIN, "admin@zest.local", "系统管理员", DEFAULT_PASSWORD, SsoConstants.ROLE_SSO_ADMIN, 1);

            log.info("已创建默认管理员账号: {}", DEFAULT_ADMIN);

            return;

        }

        if (ssoProperties.getBootstrap().isResetDefaultPassword()

                && !passwordEncoder.matches(DEFAULT_PASSWORD, admin.getPasswordHash())) {

            resetPassword(admin.getId(), DEFAULT_PASSWORD);

            log.info("已重置默认管理员密码，请登录后立即修改");

        }

    }



    private void ensureOperatorUser() {

        SsoUser operator = userMapper.selectOne(new LambdaQueryWrapper<SsoUser>()

                .eq(SsoUser::getUsername, DEFAULT_OPERATOR));

        if (operator == null) {

            createUser(DEFAULT_OPERATOR, "operator@zest.local", "运维账号", OPERATOR_PASSWORD,

                    SsoConstants.ROLE_SSO_OPERATOR, 0);

            log.info("已创建默认运维账号: {} / {}", DEFAULT_OPERATOR, OPERATOR_PASSWORD);

        }

    }



    private void createUser(String username, String email, String displayName, String password,

                            String roleCode, int superAdmin) {

        SsoUser user = new SsoUser();

        user.setUsername(username);

        user.setEmail(email);

        user.setPasswordHash(passwordEncoder.encode(password));

        user.setDisplayName(displayName);

        user.setStatus(UserStatus.ACTIVE.getCode());

        user.setIsSuperAdmin(superAdmin);

        userMapper.insert(user);



        SsoRole role = roleMapper.selectOne(new LambdaQueryWrapper<SsoRole>().eq(SsoRole::getCode, roleCode));

        if (role != null) {

            SsoUserRole ur = new SsoUserRole();

            ur.setUserId(user.getId());

            ur.setRoleId(role.getId());

            userRoleMapper.insert(ur);

        }



        SsoTenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<SsoTenant>().eq(SsoTenant::getCode, "default"));

        if (tenant != null) {

            SsoUserTenant ut = new SsoUserTenant();

            ut.setUserId(user.getId());

            ut.setTenantId(tenant.getId());

            ut.setIsDefault(1);

            userTenantMapper.insert(ut);

        }

    }



    private void resetPassword(Long userId, String password) {

        SsoUser update = new SsoUser();

        update.setId(userId);

        update.setPasswordHash(passwordEncoder.encode(password));

        userMapper.updateById(update);

    }

}

