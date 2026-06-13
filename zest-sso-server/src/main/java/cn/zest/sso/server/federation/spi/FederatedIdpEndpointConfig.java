package cn.zest.sso.server.federation.spi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 手动 OAuth2/OIDC 端点（无 Discovery 或需覆盖 Discovery 时使用）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FederatedIdpEndpointConfig {

    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
    private String jwkSetUri;
    /** 企微等需要的额外 query 参数，如 agentid */
    private String authorizationQueryParams;
}
