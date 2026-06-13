package cn.zest.sso.server.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * RFC 8628 公网客户端认证：Device Authorization / device_code Token 请求仅携带 client_id。
 */
public class DevicePublicClientAuthenticationConverter implements AuthenticationConverter {

    private static final String DEVICE_AUTHORIZATION_SUFFIX = "/oauth2/device_authorization";
    private static final String TOKEN_SUFFIX = "/oauth2/token";

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        MultiValueMap<String, String> parameters = getFormParameters(request);
        String clientId = parameters.getFirst(OAuth2ParameterNames.CLIENT_ID);
        if (!StringUtils.hasText(clientId) || parameters.get(OAuth2ParameterNames.CLIENT_ID).size() != 1) {
            return null;
        }
        if (StringUtils.hasText(parameters.getFirst(OAuth2ParameterNames.CLIENT_SECRET))) {
            return null;
        }
        if (!matchesDeviceFlowRequest(request, parameters)) {
            return null;
        }

        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach((name, values) -> {
            if (!OAuth2ParameterNames.CLIENT_ID.equals(name) && !values.isEmpty()) {
                additionalParameters.put(name, values.get(0));
            }
        });
        return new OAuth2ClientAuthenticationToken(clientId, ClientAuthenticationMethod.NONE, null, additionalParameters);
    }

    private boolean matchesDeviceFlowRequest(HttpServletRequest request, MultiValueMap<String, String> parameters) {
        String uri = request.getRequestURI();
        if (uri != null && uri.endsWith(DEVICE_AUTHORIZATION_SUFFIX)) {
            return true;
        }
        if (uri != null && uri.endsWith(TOKEN_SUFFIX)) {
            return AuthorizationGrantType.DEVICE_CODE.getValue().equals(parameters.getFirst(OAuth2ParameterNames.GRANT_TYPE));
        }
        return false;
    }

    private MultiValueMap<String, String> getFormParameters(HttpServletRequest request) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        request.getParameterMap().forEach((name, values) -> {
            for (String value : values) {
                parameters.add(name, value);
            }
        });
        return parameters;
    }
}
