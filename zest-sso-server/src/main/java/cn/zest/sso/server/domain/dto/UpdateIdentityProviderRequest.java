package cn.zest.sso.server.domain.dto;

import lombok.Data;

@Data
public class UpdateIdentityProviderRequest {

    private String displayName;
    private String discoveryUri;
    private String clientId;
    private String clientSecret;
    private String scopes;
    private String usernameClaim;
    private String emailClaim;
    private String displayNameClaim;
    private String roleClaim;
    private String defaultRoleCodes;
    private String samlEntityId;
    private String samlSsoUrl;
    private String samlMetadataUri;
    private String samlVerificationCertificate;
    private Integer enabled;
}
