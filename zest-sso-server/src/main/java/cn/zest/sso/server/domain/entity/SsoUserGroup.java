package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sso_user_group")
public class SsoUserGroup {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long groupId;
    private LocalDateTime createTime;
}
