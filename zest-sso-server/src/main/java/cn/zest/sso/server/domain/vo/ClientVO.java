package cn.zest.sso.server.domain.vo;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * OAuth 客户端视图。
 */
@Data
@SuperBuilder
public class ClientVO {

    private Long id;
    private String clientId;
    private String clientName;
    private List<String> authorizationGrantTypes;
    private List<String> redirectUris;
    private List<String> scopes;
    private Boolean requirePkce;
    private Boolean requireConsent;
    private Integer accessTokenTtl;
    private Integer refreshTokenTtl;
    private String backchannelLogoutUri;
    private String frontchannelLogoutUri;
    private Integer status;
}
