package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SettingsVO {

    private String issuer;
    private String keyId;
    private int accessTokenTtl;
    private int refreshTokenTtl;
    private int idTokenTtl;
    private int loginRateLimit;
    private int loginRateWindowSeconds;
    private int maxLoginAttempts;
    private int loginLockMinutes;
    private String adminConsolePath;
    private PasswordPolicyVO passwordPolicy;
}
