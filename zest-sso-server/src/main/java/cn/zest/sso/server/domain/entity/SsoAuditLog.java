package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计日志实体。
 */
@Data
@TableName("sso_audit_log")
public class SsoAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventType;
    private String actor;
    private String target;
    private String clientId;
    private String ipAddress;
    private String userAgent;
    private String detail;
    private LocalDateTime createTime;
}
