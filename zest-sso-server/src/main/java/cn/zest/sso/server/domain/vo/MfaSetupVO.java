package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MfaSetupVO {

    private String secret;
    private String otpAuthUrl;
    private boolean enabled;
}
