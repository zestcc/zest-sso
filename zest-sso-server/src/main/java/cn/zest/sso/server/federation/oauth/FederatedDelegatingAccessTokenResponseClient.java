package cn.zest.sso.server.federation.oauth;

import cn.zest.sso.server.federation.oauth.spi.FederatedOAuth2TokenClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FederatedDelegatingAccessTokenResponseClient
        implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private final List<FederatedOAuth2TokenClient> tokenClients;
    private final DefaultAuthorizationCodeTokenResponseClient defaultClient =
            new DefaultAuthorizationCodeTokenResponseClient();

    public FederatedDelegatingAccessTokenResponseClient(List<FederatedOAuth2TokenClient> tokenClients) {
        this.tokenClients = tokenClients;
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest request) {
        ClientRegistration registration = request.getClientRegistration();
        for (FederatedOAuth2TokenClient client : tokenClients) {
            if (client.supports(registration)) {
                return client.getTokenResponse(request);
            }
        }
        return defaultClient.getTokenResponse(request);
    }
}
