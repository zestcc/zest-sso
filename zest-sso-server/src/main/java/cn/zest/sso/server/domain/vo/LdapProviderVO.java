package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LdapProviderVO {

    private Long id;
    private String alias;
    private String displayName;
    private String serverUrl;
    private String baseDn;
    private String bindDn;
    private String userSearchBase;
    private String userSearchFilter;
    private String groupSearchBase;
    private String groupRoleAttribute;
    private Integer enabled;
}
