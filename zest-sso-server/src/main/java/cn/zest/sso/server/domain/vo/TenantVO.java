package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 租户视图。
 */
@Data
@Builder
public class TenantVO {

    private Long id;
    private String code;
    private String name;
    private Integer status;
    private Boolean isDefault;
    private Boolean system;
}
