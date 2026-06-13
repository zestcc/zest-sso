package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sso_jwt_signing_key")
public class SsoJwtSigningKey {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    @TableId(type = IdType.AUTO)
    private Long id;
    private String keyId;
    private String publicPem;
    private String privatePem;
    private String status;
    private LocalDateTime notAfter;
    private LocalDateTime createTime;
}
