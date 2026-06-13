package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLdapProviderRequest {

    @NotBlank
    private String alias;
    @NotBlank
    private String displayName;
    @NotBlank
    private String serverUrl;
    @NotBlank
    private String baseDn;
    private String bindDn;
    private String bindPassword;
    @NotBlank
    private String userSearchBase;
    private String userSearchFilter;
    private String groupSearchBase;
    private String groupRoleAttribute;
}
