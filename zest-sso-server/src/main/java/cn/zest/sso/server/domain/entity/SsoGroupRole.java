package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sso_group_role")
public class SsoGroupRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private Long roleId;
    private LocalDateTime createTime;
}
