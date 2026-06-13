package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "用户名或邮箱不能为空")
    private String usernameOrEmail;
}
