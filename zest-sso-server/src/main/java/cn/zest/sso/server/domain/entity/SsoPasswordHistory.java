package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sso_password_history")
public class SsoPasswordHistory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String passwordHash;
    private LocalDateTime createTime;
}
