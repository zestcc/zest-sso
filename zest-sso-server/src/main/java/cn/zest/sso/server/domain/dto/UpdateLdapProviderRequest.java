package cn.zest.sso.server.domain.dto;

import lombok.Data;

@Data
public class UpdateLdapProviderRequest {

    private String displayName;
    private String serverUrl;
    private String baseDn;
    private String bindDn;
    private String bindPassword;
    private String userSearchBase;
    private String userSearchFilter;
    private String groupSearchBase;
    private String groupRoleAttribute;
    private Integer enabled;
}
