package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResultVO {

    private boolean mfaRequired;
    private String mfaToken;
    /** TOTP 或 EMAIL */
    private String mfaMode;
    private UserInfoVO user;
}
