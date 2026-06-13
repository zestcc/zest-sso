package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordByTokenRequest {

    @NotBlank(message = "token 不能为空")
    private String token;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "新密码至少 8 位")
    private String newPassword;
}
