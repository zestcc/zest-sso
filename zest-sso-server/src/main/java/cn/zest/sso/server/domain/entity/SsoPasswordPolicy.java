package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sso_password_policy")
public class SsoPasswordPolicy {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer minLength;
    private Integer requireUppercase;
    private Integer requireLowercase;
    private Integer requireDigit;
    private Integer requireSpecial;
    private Integer passwordHistoryCount;
    private Integer maxAgeDays;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
