package cn.zest.sso.server.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateClientRequest {

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
}
