package cn.zest.sso.server.federation.oauth;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.federation.oauth.spi.FederatedOAuth2TokenClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 企业微信 OAuth2 换票：corp token + auth/getuserinfo。
 */
@Component
@RequiredArgsConstructor
public class WecomOAuth2TokenClient implements FederatedOAuth2TokenClient {

    private static final String CORP_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String USER_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo";

    private final SsoProperties ssoProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String adapterKey() {
        return "wecom";
    }

    @Override
    public boolean supports(ClientRegistration registration) {
        if (!ssoProperties.getModules().isWecomFederation()) {
            return false;
        }
        if (registration == null) {
            return false;
        }
        Object adapter = registration.getProviderDetails().getConfigurationMetadata().get("adapter_key");
        if (adapter instanceof String s && "wecom".equals(s)) {
            return true;
        }
        return "wecom".equals(registration.getRegistrationId());
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest request) {
        ClientRegistration registration = request.getClientRegistration();
        String code = request.getAuthorizationExchange().getAuthorizationResponse().getCode();
        if (!StringUtils.hasText(code)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "企微授权码为空");
        }
        String corpToken = fetchCorpAccessToken(registration.getClientId(), registration.getClientSecret());
        JsonNode userInfo = fetchUserInfo(corpToken, code);
        String userid = text(userInfo, "userid");
        Map<String, Object> additional = new LinkedHashMap<>();
        additional.put("userid", userid);
        if (userInfo.hasNonNull("user_ticket")) {
            additional.put("user_ticket", userInfo.get("user_ticket").asText());
        }
        if (userInfo.hasNonNull("openid")) {
            additional.put("openid", userInfo.get("openid").asText());
        }
        return OAuth2AccessTokenResponse.withToken(corpToken)
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .expiresIn(7200L)
                .additionalParameters(additional)
                .build();
    }

    private String fetchCorpAccessToken(String corpId, String corpSecret) {
        String url = UriComponentsBuilder.fromHttpUrl(CORP_TOKEN_URL)
                .queryParam("corpid", corpId)
                .queryParam("corpsecret", corpSecret)
                .toUriString();
        JsonNode body = fetchJson(url);
        assertOk(body);
        return text(body, "access_token");
    }

    private JsonNode fetchUserInfo(String accessToken, String code) {
        String url = UriComponentsBuilder.fromHttpUrl(USER_INFO_URL)
                .queryParam("access_token", accessToken)
                .queryParam("code", code)
                .toUriString();
        JsonNode body = fetchJson(url);
        assertOk(body);
        return body;
    }

    private JsonNode fetchJson(String url) {
        try {
            String raw = restTemplate.getForObject(url, String.class);
            return objectMapper.readTree(raw);
        } catch (Exception ex) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "企微 API 调用失败: " + ex.getMessage());
        }
    }

    private void assertOk(JsonNode body) {
        if (body == null || !body.has("errcode")) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "企微 API 响应无效");
        }
        if (body.get("errcode").asInt() != 0) {
            throw new SsoException(ErrorCode.BAD_REQUEST,
                    "企微 API 错误: " + body.path("errmsg").asText("unknown"));
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !StringUtils.hasText(value.asText())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "企微响应缺少字段: " + field);
        }
        return value.asText();
    }
}
