package cn.zest.sso.client.rp.oidc;

import cn.zest.sso.client.rp.RpSsoException;
import cn.zest.sso.client.rp.RpSsoProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * OIDC Authorization Code + PKCE Token 交换。
 */
@Slf4j
@RequiredArgsConstructor
public class OidcTokenClient {

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public String exchangeCodeForIdToken(String code,
                                         String codeVerifier,
                                         OidcEndpoints endpoints,
                                         RpSsoProperties props) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", props.getRedirectUri());
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("code_verifier", codeVerifier);

        try {
            String body = restClientBuilder.build()
                    .post()
                    .uri(endpoints.tokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            JsonNode json = objectMapper.readTree(body);
            String idToken = json.path("id_token").asText(null);
            if (!StringUtils.hasText(idToken)) {
                log.warn("Token 响应缺少 id_token: {}", body);
                throw new RpSsoException("TOKEN_EXCHANGE_FAILED", "SSO Token 交换失败");
            }
            return idToken;
        } catch (RpSsoException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("SSO Token 交换异常", ex);
            throw new RpSsoException("TOKEN_EXCHANGE_FAILED", "SSO Token 交换失败", ex);
        }
    }
}
