package cn.zest.sso.server.domain.dto;

import lombok.Data;

@Data
public class WebauthnRegisterOptionsRequest {
    private String nickname = "Passkey";
}
