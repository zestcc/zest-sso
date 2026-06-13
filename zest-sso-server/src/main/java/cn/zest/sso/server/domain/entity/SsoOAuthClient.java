package cn.zest.sso.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OAuth2 客户端注册实体。
 */
@Data
@TableName("sso_oauth_client")
public class SsoOAuthClient {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String clientId;
    private String clientSecretHash;
    private String clientName;
    private String clientAuthenticationMethods;
    private String authorizationGrantTypes;
    private String redirectUris;
    private String scopes;
    private Integer requirePkce;
    private Integer requireConsent;
    private Integer accessTokenTtl;
    private Integer refreshTokenTtl;
    private String backchannelLogoutUri;
    private String frontchannelLogoutUri;
    private String mtlsCertificateThumbprints;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
