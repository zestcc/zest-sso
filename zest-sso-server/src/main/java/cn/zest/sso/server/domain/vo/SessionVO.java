package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SessionVO {

    private String sessionId;
    private String username;
    private Instant creationTime;
    private Instant lastAccessedTime;
    private long maxInactiveIntervalSeconds;
    private boolean expired;
}
