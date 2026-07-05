package cn.zest.sso.client.rp;

/**
 * RP 侧 SSO 流程异常 — 接入方捕获后可映射为本地错误码。
 */
public class RpSsoException extends RuntimeException {

    private final String errorCode;

    public RpSsoException(String message) {
        this("SSO_ERROR", message);
    }

    public RpSsoException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RpSsoException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
