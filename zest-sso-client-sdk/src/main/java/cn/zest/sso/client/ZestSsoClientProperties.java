package cn.zest.sso.client;

import cn.zest.sso.client.rp.RpSsoProperties;
import cn.zest.sso.client.rp.provider.ZestSsoRpProvider;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * ZestSSO 客户端集成配置。
 */
@Data
@ConfigurationProperties(prefix = "zest.sso.client")
public class ZestSsoClientProperties implements RpSsoProperties {

    /**
     * 是否启用 SSO 集成。
     */
    private boolean enabled = false;

    /**
     * ZestSSO Issuer URL。
     */
    private String issuer = "http://localhost:9000";

    /**
     * OAuth2 Client ID。
     */
    private String clientId;

    /**
     * OAuth2 Client Secret。
     */
    private String clientSecret;

    /**
     * 授权回调地址。
     */
    private String redirectUri;

    /**
     * 请求的 Scope。
     */
    private List<String> scopes = List.of("openid", "profile", "email", "roles", "tenant");

    /**
     * JWKS 缓存时间（秒）。
     */
    private int jwksCacheSeconds = 3600;

    /**
     * 用户名 Claim 名称。
     */
    private String usernameClaim = "preferred_username";

    /**
     * 角色 Claim 名称。
     */
    private String rolesClaim = "roles";

    /**
     * 租户 Claim 名称。
     */
    private String tenantClaim = "tenant_id";

    /** Back-Channel Logout 接收路径 */
    private String backchannelLogoutPath = "/auth/backchannel-logout";

    /** Front-Channel Logout 页面路径 */
    private String frontchannelLogoutPath = "/auth/frontchannel-logout";

    private boolean backchannelLogoutEnabled = true;

    private boolean frontchannelLogoutEnabled = true;

    @Override
    public String getProvider() {
        return ZestSsoRpProvider.PROVIDER_ID;
    }
}
