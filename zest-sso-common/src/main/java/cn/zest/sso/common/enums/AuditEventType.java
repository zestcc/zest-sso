package cn.zest.sso.common.enums;

import lombok.Getter;

/**
 * 审计事件类型。
 */
@Getter
public enum AuditEventType {

    LOGIN_SUCCESS("LOGIN_SUCCESS", "登录成功"),
    LOGIN_FAILURE("LOGIN_FAILURE", "登录失败"),
    LOGOUT("LOGOUT", "登出"),
    USER_CREATE("USER_CREATE", "创建用户"),
    USER_UPDATE("USER_UPDATE", "更新用户"),
    USER_DISABLE("USER_DISABLE", "禁用用户"),
    USER_ENABLE("USER_ENABLE", "启用用户"),
    USER_UNLOCK("USER_UNLOCK", "解锁用户"),
    USER_DELETE("USER_DELETE", "删除用户"),
    CLIENT_CREATE("CLIENT_CREATE", "创建客户端"),
    CLIENT_UPDATE("CLIENT_UPDATE", "更新客户端"),
    CLIENT_DISABLE("CLIENT_DISABLE", "禁用客户端"),
    CLIENT_ENABLE("CLIENT_ENABLE", "启用客户端"),
    CLIENT_DELETE("CLIENT_DELETE", "删除客户端"),
    CLIENT_RESET_SECRET("CLIENT_RESET_SECRET", "重置客户端密钥"),
    TENANT_CREATE("TENANT_CREATE", "创建租户"),
    TENANT_UPDATE("TENANT_UPDATE", "更新租户"),
    TENANT_DISABLE("TENANT_DISABLE", "禁用租户"),
    TENANT_ENABLE("TENANT_ENABLE", "启用租户"),
    TENANT_DELETE("TENANT_DELETE", "删除租户"),
    ROLE_CREATE("ROLE_CREATE", "创建角色"),
    ROLE_UPDATE("ROLE_UPDATE", "更新角色"),
    ROLE_DELETE("ROLE_DELETE", "删除角色"),
    TOKEN_REVOKE("TOKEN_REVOKE", "吊销令牌"),
    SESSION_REVOKE("SESSION_REVOKE", "强制下线会话"),
    MFA_ENABLE("MFA_ENABLE", "启用 MFA"),
    MFA_DISABLE("MFA_DISABLE", "禁用 MFA"),
    MFA_ADMIN_RESET("MFA_ADMIN_RESET", "管理员重置 MFA"),
    IDP_CREATE("IDP_CREATE", "创建身份源"),
    IDP_UPDATE("IDP_UPDATE", "更新身份源"),
    IDP_DELETE("IDP_DELETE", "删除身份源"),
    GROUP_CREATE("GROUP_CREATE", "创建用户组"),
    GROUP_UPDATE("GROUP_UPDATE", "更新用户组"),
    GROUP_DELETE("GROUP_DELETE", "删除用户组"),
    LDAP_CREATE("LDAP_CREATE", "创建 LDAP 源"),
    LDAP_UPDATE("LDAP_UPDATE", "更新 LDAP 源"),
    LDAP_DELETE("LDAP_DELETE", "删除 LDAP 源"),
    PASSWORD_POLICY_UPDATE("PASSWORD_POLICY_UPDATE", "更新密码策略"),
    PASSWORD_CHANGE("PASSWORD_CHANGE", "修改密码"),
    PASSWORD_RESET_REQUEST("PASSWORD_RESET_REQUEST", "请求密码重置"),
    PASSWORD_RESET_COMPLETE("PASSWORD_RESET_COMPLETE", "完成密码重置"),
    WEBAUTHN_REGISTER("WEBAUTHN_REGISTER", "注册 Passkey"),
    WEBAUTHN_DELETE("WEBAUTHN_DELETE", "删除 Passkey"),
    WEBAUTHN_LOGIN("WEBAUTHN_LOGIN", "Passkey 登录");

    private final String code;
    private final String label;

    AuditEventType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
