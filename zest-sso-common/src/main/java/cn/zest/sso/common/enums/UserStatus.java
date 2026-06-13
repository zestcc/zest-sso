package cn.zest.sso.common.enums;

import lombok.Getter;

/**
 * 用户状态枚举。
 */
@Getter
public enum UserStatus {

    ACTIVE(1, "正常"),
    DISABLED(0, "禁用"),
    LOCKED(2, "锁定");

    private final int code;
    private final String label;

    UserStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static UserStatus fromCode(int code) {
        for (UserStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown user status: " + code);
    }
}
