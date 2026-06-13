package cn.zest.sso.server.federation.oauth.spi;

import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

/**
 * 联邦 OAuth2 换票插拔客户端 — 用于企微等非标准 OIDC 平台。
 */
public interface FederatedOAuth2TokenClient {

    String adapterKey();

    boolean supports(ClientRegistration registration);

    OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest request);
}
