package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * SSO 用户实体。
 */
@Data
@TableName("sso_user")
public class SsoUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String email;
    private String passwordHash;
    private String displayName;
    private Integer status;
    private Integer isSuperAdmin;
    private Integer mfaEnabled;
    private String mfaSecret;
    private LocalDateTime passwordChangedAt;
    private String externalId;
    private String federationSource;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private LocalDateTime inactiveDisabledAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
