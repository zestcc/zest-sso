package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sso_plugin_config")
public class SsoPluginConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String pluginKey;
    private Integer enabled;
    private String config;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
