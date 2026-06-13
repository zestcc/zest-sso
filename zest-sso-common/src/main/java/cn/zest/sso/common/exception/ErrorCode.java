package cn.zest.sso.common.exception;

/**
 * 错误码定义。
 */
public final class ErrorCode {

    private ErrorCode() {
    }

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int INTERNAL_ERROR = 500;

    public static final int USER_NOT_FOUND = 1001;
    public static final int USER_DISABLED = 1002;
    public static final int USER_LOCKED = 1003;
    public static final int INVALID_CREDENTIALS = 1004;
    public static final int CLIENT_NOT_FOUND = 2001;
    public static final int TENANT_NOT_FOUND = 3001;
}
