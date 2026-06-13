package cn.zest.sso.server.support;

import cn.zest.sso.common.constant.SsoConstants;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;

import java.util.Set;

public final class AdminRbacSupport {

    private static final Set<String> SYSTEM_ROLES = Set.of(
            SsoConstants.ROLE_SSO_ADMIN,
            SsoConstants.ROLE_SSO_OPERATOR,
            SsoConstants.ROLE_TENANT_ADMIN,
            SsoConstants.ROLE_USER);

    private static final Set<String> PRIVILEGED_ROLES = Set.of(
            SsoConstants.ROLE_SSO_ADMIN,
            SsoConstants.ROLE_SSO_OPERATOR,
            SsoConstants.ROLE_TENANT_ADMIN);

    private AdminRbacSupport() {
    }

    public static boolean isSystemRole(String code) {
        return SYSTEM_ROLES.contains(code);
    }

    public static void assertOperatorCanAssignRoles(boolean isAdmin, java.util.List<String> roleCodes) {
        if (isAdmin || roleCodes == null) {
            return;
        }
        for (String code : roleCodes) {
            if (PRIVILEGED_ROLES.contains(code)) {
                throw new SsoException(ErrorCode.FORBIDDEN, "运维账号不能分配管理员或运维角色");
            }
        }
    }

    public static void assertOperatorCanManageUser(boolean isAdmin, java.util.List<String> targetRoles) {
        if (isAdmin || targetRoles == null) {
            return;
        }
        for (String code : targetRoles) {
            if (PRIVILEGED_ROLES.contains(code)) {
                throw new SsoException(ErrorCode.FORBIDDEN, "无权管理该用户");
            }
        }
    }

    public static void assertTenantAdminCanAssignRoles(boolean tenantAdminOnly, java.util.List<String> roleCodes) {
        if (!tenantAdminOnly || roleCodes == null) {
            return;
        }
        for (String code : roleCodes) {
            if (!SsoConstants.ROLE_USER.equals(code)) {
                throw new SsoException(ErrorCode.FORBIDDEN, "租户管理员只能分配普通用户角色");
            }
        }
    }
}
