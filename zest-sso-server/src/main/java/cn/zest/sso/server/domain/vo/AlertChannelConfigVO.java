package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AlertChannelConfigVO {
    private Long id;
    private String name;
    private String channelKey;
    private Integer enabled;
    private List<String> events;
    private Map<String, String> config;
}
