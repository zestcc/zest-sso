package cn.zest.sso.client.rp;

import io.jsonwebtoken.Claims;
import lombok.Builder;
import lombok.Data;

/**
 * OIDC 回调校验结果 — 仅含 IdP Claims，本地 JWT 由接入方签发。
 */
@Data
@Builder
public class RpSsoCallbackResult {
    private String providerId;
    private Claims claims;
    private String idToken;
}
