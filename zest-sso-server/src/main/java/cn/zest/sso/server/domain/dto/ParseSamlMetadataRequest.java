package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParseSamlMetadataRequest {

    @NotBlank(message = "metadataUri 不能为空")
    private String metadataUri;
}
