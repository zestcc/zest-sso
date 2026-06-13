package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class WebauthnFinishRequest {

    @NotBlank
    private String sessionToken;

    @NotNull
    private Map<String, Object> credential;

    private String nickname;
}
