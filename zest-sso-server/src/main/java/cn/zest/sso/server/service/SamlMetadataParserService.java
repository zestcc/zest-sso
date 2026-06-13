package cn.zest.sso.server.service;

import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.vo.SamlMetadataVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Slf4j
@Service
public class SamlMetadataParserService {

    public SamlMetadataVO parseFromUri(String metadataUri) {
        if (!StringUtils.hasText(metadataUri)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "metadataUri 不能为空");
        }
        try {
            RelyingPartyRegistration registration = RelyingPartyRegistrations
                    .fromMetadataLocation(metadataUri.trim())
                    .registrationId("metadata-import")
                    .entityId("https://placeholder.local/saml2/service-provider-metadata/metadata-import")
                    .assertionConsumerServiceLocation("https://placeholder.local/login/saml2/sso/metadata-import")
                    .build();
            return toMetadataVO(registration, metadataUri.trim());
        } catch (Exception e) {
            log.warn("SAML 元数据解析失败: {}", metadataUri, e);
            throw new SsoException(ErrorCode.BAD_REQUEST, "无法解析 SAML 元数据: " + e.getMessage());
        }
    }

    public SamlMetadataVO parseFromXml(String metadataXml) {
        if (!StringUtils.hasText(metadataXml)) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "元数据 XML 不能为空");
        }
        try (InputStream input = new ByteArrayInputStream(stripBom(metadataXml).getBytes(StandardCharsets.UTF_8))) {
            RelyingPartyRegistration registration = RelyingPartyRegistrations
                    .fromMetadata(input)
                    .registrationId("metadata-import")
                    .entityId("https://placeholder.local/saml2/service-provider-metadata/metadata-import")
                    .assertionConsumerServiceLocation("https://placeholder.local/login/saml2/sso/metadata-import")
                    .build();
            return toMetadataVO(registration, null);
        } catch (Exception e) {
            log.warn("SAML 元数据 XML 解析失败", e);
            throw new SsoException(ErrorCode.BAD_REQUEST, "无法解析 SAML 元数据 XML: " + e.getMessage());
        }
    }

    private SamlMetadataVO toMetadataVO(RelyingPartyRegistration registration, String metadataUri) {
        var assertingParty = registration.getAssertingPartyDetails();
        String certificate = assertingParty.getVerificationX509Credentials().stream()
                .findFirst()
                .map(Saml2X509Credential::getCertificate)
                .map(this::toPem)
                .orElseThrow(() -> new SsoException(ErrorCode.BAD_REQUEST, "元数据中未找到 IdP 签名证书"));
        return SamlMetadataVO.builder()
                .entityId(assertingParty.getEntityId())
                .ssoUrl(assertingParty.getSingleSignOnServiceLocation())
                .verificationCertificate(certificate)
                .metadataUri(metadataUri)
                .build();
    }

    private String stripBom(String text) {
        if (text != null && text.startsWith("\uFEFF")) {
            return text.substring(1);
        }
        return text != null ? text.trim() : "";
    }

    private String toPem(X509Certificate certificate) {
        try {
            String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'})
                    .encodeToString(certificate.getEncoded());
            return "-----BEGIN CERTIFICATE-----\n" + encoded + "\n-----END CERTIFICATE-----";
        } catch (Exception e) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "证书编码失败");
        }
    }
}
