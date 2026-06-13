package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupRequest {

    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String description;
    private List<String> roleCodes;
}
