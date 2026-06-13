package cn.zest.sso.client;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OIDC Discovery 元数据。
 */
@Data
@Builder
public class OidcDiscoveryMetadata {

    private String issuer;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userinfoEndpoint;
    private String jwksUri;
    private String revocationEndpoint;
    private String endSessionEndpoint;
    private List<String> scopesSupported;
    private List<String> grantTypesSupported;
}
