package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class WebauthnOptionsVO {
    private String sessionToken;
    private Map<String, Object> publicKey;
}
