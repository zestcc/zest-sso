package cn.zest.sso.server.service;

import cn.zest.sso.server.domain.vo.SamlMetadataVO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SamlMetadataParserTest {

    private final SamlMetadataParserService samlMetadataParserService = new SamlMetadataParserService();

    @Test
    void shouldParseSamlMetadataXml() throws Exception {
        String xml = new ClassPathResource("saml/test-idp-metadata.xml")
                .getContentAsString(StandardCharsets.UTF_8);
        SamlMetadataVO metadata = samlMetadataParserService.parseFromXml(xml);
        assertThat(metadata.getEntityId()).isEqualTo("https://idp.example.com/metadata");
        assertThat(metadata.getSsoUrl()).isEqualTo("https://idp.example.com/sso/saml");
        assertThat(metadata.getVerificationCertificate()).contains("BEGIN CERTIFICATE");
    }

    @Test
    void shouldParseSamlMetadataFromFileUri() throws Exception {
        String metadataUri = new ClassPathResource("saml/test-idp-metadata.xml").getFile().toURI().toString();
        SamlMetadataVO metadata = samlMetadataParserService.parseFromUri(metadataUri);
        assertThat(metadata.getEntityId()).isEqualTo("https://idp.example.com/metadata");
    }
}
