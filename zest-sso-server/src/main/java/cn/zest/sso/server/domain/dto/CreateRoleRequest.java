package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateRoleRequest {

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,62}$", message = "角色编码需大写字母开头，仅含大写字母、数字、下划线")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    private String name;

    private String description;
}
