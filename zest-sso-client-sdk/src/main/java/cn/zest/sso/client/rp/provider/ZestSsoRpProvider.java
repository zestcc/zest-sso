package cn.zest.sso.client.rp.provider;

import cn.zest.sso.client.rp.RpSsoException;
import cn.zest.sso.client.rp.RpSsoProperties;
import cn.zest.sso.client.rp.oidc.OidcEndpointResolver;
import cn.zest.sso.client.rp.oidc.OidcJwtValidator;
import cn.zest.sso.client.rp.oidc.OidcTokenClient;
import cn.zest.sso.client.rp.oidc.PkceUtils;
import cn.zest.sso.client.rp.store.RpPkceStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * ZestSSO 提供方 — 默认 Discovery + logout-url API。
 */
@Slf4j
public class ZestSsoRpProvider extends AbstractOidcRpProvider {

    public static final String PROVIDER_ID = "zest-sso";

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public ZestSsoRpProvider(RpPkceStore pkceStore,
                             OidcEndpointResolver endpointResolver,
                             OidcTokenClient tokenClient,
                             OidcJwtValidator jwtValidator,
                             RestClient.Builder restClientBuilder,
                             ObjectMapper objectMapper) {
        super(pkceStore, endpointResolver, tokenClient, jwtValidator);
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    protected String displayName(RpSsoProperties props) {
        return "ZestSSO";
    }

    @Override
    public String buildLogoutUrl(RpSsoProperties props) {
        if (props.isZestSsoUseLogoutUrlApi()) {
            return fetchLogoutUrlFromApi(props, props.getZestSsoLogoutUrlApiPath());
        }
        return super.buildLogoutUrl(props);
    }

    private String fetchLogoutUrlFromApi(RpSsoProperties props, String apiPath) {
        String base = props.getIssuer().replaceAll("/$", "");
        String path = StringUtils.hasText(apiPath) ? apiPath : "/api/public/logout-url";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String redirect = props.getPostLogoutRedirectUri();
        try {
            String body = restClientBuilder.build()
                    .get()
                    .uri(base + path + "?redirect_uri=" + PkceUtils.urlEncode(redirect))
                    .retrieve()
                    .body(String.class);
            return parseLogoutUrlResponse(body, objectMapper);
        } catch (RpSsoException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("ZestSSO logout-url 调用异常", ex);
            throw new RpSsoException("LOGOUT_URL_FAILED", "SSO 登出 URL 获取失败", ex);
        }
    }

    /** 解析 ZestSSO ApiResponse&lt;String&gt; 登出 URL */
    public static String parseLogoutUrlResponse(String body, ObjectMapper objectMapper) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.path("code").asInt(-1) != 0) {
                throw new RpSsoException("LOGOUT_URL_FAILED", "SSO 登出 URL 获取失败");
            }
            String url = root.path("data").asText(null);
            if (!StringUtils.hasText(url)) {
                throw new RpSsoException("LOGOUT_URL_FAILED", "SSO 登出 URL 获取失败");
            }
            return url;
        } catch (RpSsoException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RpSsoException("LOGOUT_URL_FAILED", "SSO 登出 URL 获取失败", ex);
        }
    }
}
