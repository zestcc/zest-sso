package cn.zest.sso.server.domain.dto;

import lombok.Data;

@Data
public class UpdateTenantRequest {

    private String name;
    private Integer status;
}
