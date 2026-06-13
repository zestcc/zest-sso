package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sso_ldap_provider")
public class SsoLdapProvider {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String alias;
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
