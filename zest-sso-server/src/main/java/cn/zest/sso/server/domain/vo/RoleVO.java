package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleVO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean system;
}
