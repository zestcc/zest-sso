package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuthorizationVO {

    private String id;
    private String clientId;
    private String principalName;
    private String grantType;
    private String scopes;
    private LocalDateTime accessTokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt;
    private boolean active;
}
