package cn.zest.sso.server.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建 OAuth 客户端请求。
 */
@Data
public class CreateClientRequest {

    @NotBlank(message = "clientId 不能为空")
    private String clientId;

    @NotBlank(message = "clientSecret 不能为空")
    @Size(min = 8, message = "clientSecret 至少 8 位")
    private String clientSecret;

    @NotBlank(message = "客户端名称不能为空")
    private String clientName;

    private List<String> authorizationGrantTypes;
    private List<String> redirectUris;
    private List<String> scopes;
    private Boolean requirePkce;
    private Boolean requireConsent;
    private Integer accessTokenTtl;
    private Integer refreshTokenTtl;
    private String backchannelLogoutUri;
    private String frontchannelLogoutUri;
}
