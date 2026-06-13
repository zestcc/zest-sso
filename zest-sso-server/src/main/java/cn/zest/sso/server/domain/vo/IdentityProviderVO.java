package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IdentityProviderVO {

    private Long id;
    private String alias;
    private String displayName;
    private String providerType;
    private String adapterKey;
    private String discoveryUri;
    private String clientId;
    private String scopes;
    private String usernameClaim;
    private String emailClaim;
    private String displayNameClaim;
    private String roleClaim;
    private String defaultRoleCodes;
    private String samlEntityId;
    private String samlSsoUrl;
    private String samlMetadataUri;
    private Integer enabled;
    private String loginUrl;
}
