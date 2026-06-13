package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sso_identity_provider")
public class SsoIdentityProvider {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String alias;
    private String displayName;
    private String providerType;
    private String discoveryUri;
    private String clientId;
    private String clientSecret;
    private String scopes;
    private String usernameClaim;
    private String emailClaim;
    private String displayNameClaim;
    private String roleClaim;
    private String defaultRoleCodes;
    private String samlEntityId;
    private String samlSsoUrl;
    private String samlVerificationCertificate;
    private String samlMetadataUri;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
