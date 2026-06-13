package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateTenantRequest {

    @NotBlank(message = "租户编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_-]{1,62}$", message = "租户编码仅支持小写字母、数字、下划线、连字符")
    private String code;

    @NotBlank(message = "租户名称不能为空")
    private String name;
}
