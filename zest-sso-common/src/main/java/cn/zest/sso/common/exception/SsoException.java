package cn.zest.sso.common.exception;

import lombok.Getter;

/**
 * SSO 业务异常。
 */
@Getter
public class SsoException extends RuntimeException {

    private final int code;

    public SsoException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SsoException(String message) {
        this(400, message);
    }
}
