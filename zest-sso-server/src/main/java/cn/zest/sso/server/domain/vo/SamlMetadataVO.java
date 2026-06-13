package cn.zest.sso.server.domain.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SamlMetadataVO {

    private String entityId;
    private String ssoUrl;
    private String verificationCertificate;
    private String metadataUri;
}
