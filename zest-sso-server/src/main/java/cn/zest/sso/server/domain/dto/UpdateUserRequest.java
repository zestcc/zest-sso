package cn.zest.sso.server.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {

    private String email;
    private String displayName;
    private Integer status;
    private List<String> roleCodes;
    private List<Long> tenantIds;
    private Long defaultTenantId;
    private List<Long> groupIds;
}
