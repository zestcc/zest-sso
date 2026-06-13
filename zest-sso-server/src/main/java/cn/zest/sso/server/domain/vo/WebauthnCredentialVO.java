package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class WebauthnCredentialVO {
    private Long id;
    private String nickname;
    private String transports;
    private LocalDateTime createTime;
    private LocalDateTime lastUsedAt;
}
