package cn.zest.sso.client;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 从 ZestSSO JWT 解析的用户身份。
 */
@Data
@Builder
public class SsoUserPrincipal {

    private String subject;
    private Long userId;
    private String username;
    private String email;
    private String displayName;
    private List<String> roles;
    private Long tenantId;
}
