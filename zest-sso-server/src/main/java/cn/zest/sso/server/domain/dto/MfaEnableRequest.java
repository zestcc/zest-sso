package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaEnableRequest {

    @NotBlank(message = "验证码不能为空")
    private String code;
}
