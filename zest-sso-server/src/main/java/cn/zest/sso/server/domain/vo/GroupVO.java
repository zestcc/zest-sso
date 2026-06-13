package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupVO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private List<String> roleCodes;
    private Long memberCount;
}
