package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sso_alert_channel")
public class SsoAlertChannel {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String channelKey;
    private Integer enabled;
    private String events;
    private String config;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
