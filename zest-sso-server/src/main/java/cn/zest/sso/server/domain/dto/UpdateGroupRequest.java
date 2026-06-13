package cn.zest.sso.server.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateGroupRequest {

    private String name;
    private String description;
    private List<String> roleCodes;
}
