package cn.zest.sso.client;

import lombok.Builder;
import lombok.Data;

/**
 * OIDC 授权请求参数。
 */
@Data
@Builder
public class AuthorizationRequest {

    private String authorizationUrl;
    private String state;
    private String codeVerifier;
}
