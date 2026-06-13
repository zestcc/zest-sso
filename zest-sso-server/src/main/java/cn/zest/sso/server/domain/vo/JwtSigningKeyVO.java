package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JwtSigningKeyVO {

    private Long id;
    private String keyId;
    private String status;
    private LocalDateTime notAfter;
    private LocalDateTime createTime;
}
