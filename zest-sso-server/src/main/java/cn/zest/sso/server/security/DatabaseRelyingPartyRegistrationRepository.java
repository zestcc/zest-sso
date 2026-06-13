package cn.zest.sso.server.security;

import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.domain.mapper.SsoIdentityProviderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseRelyingPartyRegistrationRepository
        implements RelyingPartyRegistrationRepository, Iterable<RelyingPartyRegistration> {

    private final SsoIdentityProviderMapper identityProviderMapper;

    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        SsoIdentityProvider provider = identityProviderMapper.selectOne(new LambdaQueryWrapper<SsoIdentityProvider>()
                .eq(SsoIdentityProvider::getAlias, registrationId)
                .eq(SsoIdentityProvider::getProviderType, "SAML")
                .eq(SsoIdentityProvider::getEnabled, 1));
        if (provider == null) {
            return null;
        }
        return toRegistration(provider);
    }

    @Override
    public Iterator<RelyingPartyRegistration> iterator() {
        List<RelyingPartyRegistration> registrations = new ArrayList<>();
        identityProviderMapper.selectList(new LambdaQueryWrapper<SsoIdentityProvider>()
                        .eq(SsoIdentityProvider::getProviderType, "SAML")
                        .eq(SsoIdentityProvider::getEnabled, 1))
                .forEach(provider -> registrations.add(toRegistration(provider)));
        return registrations.iterator();
    }

    private RelyingPartyRegistration toRegistration(SsoIdentityProvider provider) {
        return RelyingPartyRegistration.withRegistrationId(provider.getAlias())
                .assertingPartyDetails(party -> party
                        .entityId(provider.getSamlEntityId())
                        .singleSignOnServiceLocation(provider.getSamlSsoUrl())
                        .verificationX509Credentials(credentials ->
                                credentials.add(verificationCredential(provider.getSamlVerificationCertificate())))
                )
                .entityId("{baseUrl}/saml2/service-provider-metadata/{registrationId}")
                .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/{registrationId}")
                .build();
    }

    private Saml2X509Credential verificationCredential(String pem) {
        return Saml2X509Credential.verification(parseCertificate(pem));
    }

    private X509Certificate parseCertificate(String pem) {
        try {
            String normalized = pem.replace("-----BEGIN CERTIFICATE-----", "")
                    .replace("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(normalized);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("无效的 SAML 验证证书", e);
        }
    }
}
