package cn.zest.sso.client.rp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RpSsoPublicConfig {
    private boolean enabled;
    private String provider;
    private String displayName;
    private String issuer;
    private String clientId;
}
