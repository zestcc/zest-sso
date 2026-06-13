package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaVerifyRequest {

    @NotBlank(message = "MFA Token 不能为空")
    private String mfaToken;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
