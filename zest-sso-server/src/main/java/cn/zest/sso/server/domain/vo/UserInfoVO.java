package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息视图。
 */
@Data
@Builder
public class UserInfoVO {

    private Long id;
    private String username;
    private String email;
    private String displayName;
    private Integer status;
    private Boolean superAdmin;
    private List<String> roles;
    private List<String> groups;
    private List<Long> groupIds;
    private List<TenantVO> tenants;
    private Long defaultTenantId;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private Boolean mfaEnabled;
}
