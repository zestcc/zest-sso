package cn.zest.sso.server.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Map;

/**
 * 校验 Device Flow 公网客户端（{@link ClientAuthenticationMethod#NONE}），不强制 PKCE code_verifier。
 */
@RequiredArgsConstructor
public class DevicePublicClientAuthenticationProvider implements AuthenticationProvider {

    private final RegisteredClientRepository registeredClientRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2ClientAuthenticationToken clientAuthentication = (OAuth2ClientAuthenticationToken) authentication;
        if (!ClientAuthenticationMethod.NONE.equals(clientAuthentication.getClientAuthenticationMethod())) {
            return null;
        }
        if (!isDeviceFlowRequest(clientAuthentication.getAdditionalParameters())) {
            return null;
        }

        String clientId = clientAuthentication.getPrincipal().toString();
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw invalidClient(OAuth2ParameterNames.CLIENT_ID);
        }
        if (!registeredClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)) {
            throw invalidClient("authentication_method");
        }
        if (!registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.DEVICE_CODE)) {
            throw invalidClient(OAuth2ParameterNames.CLIENT_ID);
        }

        return new OAuth2ClientAuthenticationToken(registeredClient,
                ClientAuthenticationMethod.NONE, clientAuthentication.getCredentials());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private boolean isDeviceFlowRequest(Map<String, Object> additionalParameters) {
        if (additionalParameters == null || additionalParameters.isEmpty()) {
            return true;
        }
        if (additionalParameters.containsKey("code_verifier")) {
            return false;
        }
        Object grantType = additionalParameters.get(OAuth2ParameterNames.GRANT_TYPE);
        if (grantType == null) {
            return true;
        }
        return AuthorizationGrantType.DEVICE_CODE.getValue().equals(grantType.toString());
    }

    private OAuth2AuthenticationException invalidClient(String parameterName) {
        OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, null,
                "https://datatracker.ietf.org/doc/html/rfc6749#section-3.2.1");
        throw new OAuth2AuthenticationException(error);
    }
}
