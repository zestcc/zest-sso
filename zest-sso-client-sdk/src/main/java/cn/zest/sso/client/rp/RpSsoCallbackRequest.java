package cn.zest.sso.client.rp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RpSsoCallbackRequest {
    private String code;
    private String state;
}
