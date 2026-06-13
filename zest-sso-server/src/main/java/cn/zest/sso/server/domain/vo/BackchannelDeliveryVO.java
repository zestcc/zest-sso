package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BackchannelDeliveryVO {
    private Long id;
    private String principalName;
    private String clientId;
    private String backchannelUri;
    private String status;
    private Integer attemptCount;
    private Integer lastHttpStatus;
    private String lastError;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createTime;
}
