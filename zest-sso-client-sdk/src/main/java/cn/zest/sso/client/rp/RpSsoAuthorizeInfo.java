package cn.zest.sso.client.rp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RpSsoAuthorizeInfo {
    private String authorizationUrl;
    private String state;
}
