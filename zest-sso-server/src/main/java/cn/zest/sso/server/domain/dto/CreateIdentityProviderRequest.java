package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateIdentityProviderRequest {

    @NotBlank(message = "alias 不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_-]{1,62}$", message = "alias 仅支持小写字母、数字、下划线、连字符")
    private String alias;

    @NotBlank(message = "显示名称不能为空")
    private String displayName;

    @Pattern(regexp = "^(OIDC|SAML)$", message = "providerType 仅支持 OIDC 或 SAML")
    private String providerType;

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
}
