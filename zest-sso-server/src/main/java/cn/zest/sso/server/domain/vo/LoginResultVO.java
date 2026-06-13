package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResultVO {

    private boolean mfaRequired;
    private String mfaToken;
    /** TOTP / EMAIL / ALIYUN_SMS / TENCENT_SMS */
    private String mfaMode;
    /** step-up 提示，如邮箱/手机号掩码 */
    private String mfaHint;
    private UserInfoVO user;
}
