package cn.zest.sso.common.constant;

/**
 * SSO 系统常量。
 */
public final class SsoConstants {

    private SsoConstants() {
    }

    public static final String ISSUER_PATH = "/";

    public static final String CLAIM_USER_ID = "user_id";
    public static final String CLAIM_USERNAME = "preferred_username";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_GROUPS = "groups";
    public static final String CLAIM_TENANT_ID = "tenant_id";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_NAME = "name";

    public static final String ROLE_SSO_ADMIN = "SSO_ADMIN";
    public static final String ROLE_SSO_OPERATOR = "SSO_OPERATOR";
    public static final String ROLE_TENANT_ADMIN = "TENANT_ADMIN";
    public static final String ROLE_USER = "USER";

    public static final String REDIS_PASSWORD_RESET_PREFIX = "sso:password-reset:";
    public static final String REDIS_WEBAUTHN_CHALLENGE_PREFIX = "sso:webauthn:challenge:";

    public static final String CLIENT_ZESTFLOW_ADMIN = "zestflow-admin";
    public static final String CLIENT_ZEST_LLM_ADMIN = "zest-llm-admin";
    public static final String CLIENT_SCIM_PROVISIONER = "scim-provisioner";

    public static final String SCOPE_SCIM = "scim";

    public static final String REDIS_TOKEN_BLACKLIST_PREFIX = "sso:token:blacklist:";
    public static final String REDIS_LOGIN_ATTEMPT_PREFIX = "sso:login:attempt:";
    public static final String REDIS_SESSION_PREFIX = "sso:session:";

    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final int LOGIN_LOCK_MINUTES = 15;
}
