package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class WebauthnOptionsVO {
    private String sessionToken;
    private Map<String, Object> publicKey;
    /** 指定用户名时返回：该用户是否已注册 Passkey */
    private Boolean credentialAvailable;
}
