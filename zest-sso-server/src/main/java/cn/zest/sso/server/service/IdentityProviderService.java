package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.config.SsoProperties;
import cn.zest.sso.server.domain.dto.CreateIdentityProviderRequest;
import cn.zest.sso.server.domain.dto.UpdateIdentityProviderRequest;
import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.domain.mapper.SsoIdentityProviderMapper;
import cn.zest.sso.server.domain.vo.IdentityProviderVO;
import cn.zest.sso.server.domain.vo.SamlMetadataVO;
import cn.zest.sso.server.support.AdminAuditSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IdentityProviderService {

    private final SsoIdentityProviderMapper identityProviderMapper;
    private final AdminAuditSupport auditSupport;
    private final SsoProperties ssoProperties;
    private final SamlMetadataParserService samlMetadataParserService;

    public Page<IdentityProviderVO> pageProviders(int page, int size) {
        Page<SsoIdentityProvider> providerPage = identityProviderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<SsoIdentityProvider>().orderByDesc(SsoIdentityProvider::getCreateTime));
        Page<IdentityProviderVO> result = new Page<>(page, size, providerPage.getTotal());
        result.setRecords(providerPage.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    public List<IdentityProviderVO> listEnabledPublic() {
        return identityProviderMapper.selectList(new LambdaQueryWrapper<SsoIdentityProvider>()
                        .eq(SsoIdentityProvider::getEnabled, 1)
                        .orderByAsc(SsoIdentityProvider::getAlias))
                .stream()
                .map(this::toPublicVO)
                .toList();
    }

    public IdentityProviderVO getById(Long id) {
        return toVO(findById(id));
    }

    public SamlMetadataVO parseSamlMetadata(String metadataUri) {
        return samlMetadataParserService.parseFromUri(metadataUri);
    }

    @Transactional(rollbackFor = Exception.class)
    public IdentityProviderVO create(CreateIdentityProviderRequest request) {
        Long count = identityProviderMapper.selectCount(new LambdaQueryWrapper<SsoIdentityProvider>()
                .eq(SsoIdentityProvider::getAlias, request.getAlias()));
        if (count > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "alias 已存在");
        }
        String providerType = normalizeType(request.getProviderType());
        validateCreateRequest(request, providerType);

        SsoIdentityProvider provider = new SsoIdentityProvider();
        provider.setAlias(request.getAlias());
        provider.setDisplayName(request.getDisplayName());
        provider.setProviderType(providerType);
        applyClaimMapping(provider, request.getUsernameClaim(), request.getEmailClaim(),
                request.getDisplayNameClaim(), request.getRoleClaim(), request.getDefaultRoleCodes());
        if ("SAML".equals(providerType)) {
            applySamlFields(provider, request.getSamlMetadataUri(), request.getSamlEntityId(),
                    request.getSamlSsoUrl(), request.getSamlVerificationCertificate());
            provider.setScopes("saml");
        } else {
            provider.setDiscoveryUri(request.getDiscoveryUri());
            provider.setClientId(request.getClientId());
            provider.setClientSecret(request.getClientSecret());
            provider.setScopes(request.getScopes() != null ? request.getScopes() : "openid,profile,email");
        }
        provider.setEnabled(1);
        identityProviderMapper.insert(provider);
        auditSupport.log(AuditEventType.IDP_CREATE, provider.getAlias(), provider.getDisplayName());
        return toVO(provider);
    }

    @Transactional(rollbackFor = Exception.class)
    public IdentityProviderVO update(Long id, UpdateIdentityProviderRequest request) {
        SsoIdentityProvider provider = findById(id);
        if (request.getDisplayName() != null) {
            provider.setDisplayName(request.getDisplayName());
        }
        if ("SAML".equalsIgnoreCase(provider.getProviderType())) {
            if (request.getSamlMetadataUri() != null && !request.getSamlMetadataUri().isBlank()) {
                applySamlFields(provider, request.getSamlMetadataUri(), null, null, null);
            } else {
                if (request.getSamlEntityId() != null) {
                    provider.setSamlEntityId(request.getSamlEntityId());
                }
                if (request.getSamlSsoUrl() != null) {
                    provider.setSamlSsoUrl(request.getSamlSsoUrl());
                }
                if (request.getSamlVerificationCertificate() != null && !request.getSamlVerificationCertificate().isBlank()) {
                    provider.setSamlVerificationCertificate(request.getSamlVerificationCertificate());
                }
            }
        } else {
            if (request.getDiscoveryUri() != null) {
                provider.setDiscoveryUri(request.getDiscoveryUri());
            }
            if (request.getClientId() != null) {
                provider.setClientId(request.getClientId());
            }
            if (request.getClientSecret() != null && !request.getClientSecret().isBlank()) {
                provider.setClientSecret(request.getClientSecret());
            }
            if (request.getScopes() != null) {
                provider.setScopes(request.getScopes());
            }
        }
        applyClaimMapping(provider, request.getUsernameClaim(), request.getEmailClaim(),
                request.getDisplayNameClaim(), request.getRoleClaim(), request.getDefaultRoleCodes());
        if (request.getEnabled() != null) {
            provider.setEnabled(request.getEnabled());
        }
        identityProviderMapper.updateById(provider);
        auditSupport.log(AuditEventType.IDP_UPDATE, provider.getAlias(), provider.getDisplayName());
        return toVO(provider);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SsoIdentityProvider provider = findById(id);
        identityProviderMapper.deleteById(id);
        auditSupport.log(AuditEventType.IDP_DELETE, provider.getAlias(), provider.getDisplayName());
    }

    public SsoIdentityProvider findEnabledByAlias(String alias) {
        SsoIdentityProvider provider = identityProviderMapper.selectOne(new LambdaQueryWrapper<SsoIdentityProvider>()
                .eq(SsoIdentityProvider::getAlias, alias)
                .eq(SsoIdentityProvider::getEnabled, 1));
        if (provider == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "身份提供商不存在或未启用");
        }
        return provider;
    }

    private SsoIdentityProvider findById(Long id) {
        SsoIdentityProvider provider = identityProviderMapper.selectById(id);
        if (provider == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "身份提供商不存在");
        }
        return provider;
    }

    private void validateCreateRequest(CreateIdentityProviderRequest request, String providerType) {
        if ("SAML".equals(providerType)) {
            if (StringUtils.hasText(request.getSamlMetadataUri())) {
                return;
            }
            if (!StringUtils.hasText(request.getSamlEntityId())) {
                throw new SsoException(ErrorCode.BAD_REQUEST, "SAML Entity ID 不能为空");
            }
            if (!StringUtils.hasText(request.getSamlSsoUrl())) {
                throw new SsoException(ErrorCode.BAD_REQUEST, "SAML SSO URL 不能为空");
            }
            if (!StringUtils.hasText(request.getSamlVerificationCertificate())) {
                throw new SsoException(ErrorCode.BAD_REQUEST, "SAML 验证证书不能为空");
            }
            return;
        }
        if (!StringUtils.hasText(request.getDiscoveryUri())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "Discovery URI 不能为空");
        }
        if (!StringUtils.hasText(request.getClientId())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "Client ID 不能为空");
        }
        if (!StringUtils.hasText(request.getClientSecret())) {
            throw new SsoException(ErrorCode.BAD_REQUEST, "Client Secret 不能为空");
        }
    }

    private void applyClaimMapping(SsoIdentityProvider provider, String usernameClaim, String emailClaim,
                                   String displayNameClaim, String roleClaim, String defaultRoleCodes) {
        if (usernameClaim != null) {
            provider.setUsernameClaim(defaultClaim(usernameClaim, "SAML".equals(provider.getProviderType()) ? "uid" : "preferred_username"));
        }
        if (emailClaim != null) {
            provider.setEmailClaim(defaultClaim(emailClaim, "email"));
        }
        if (displayNameClaim != null) {
            provider.setDisplayNameClaim(defaultClaim(displayNameClaim, "SAML".equals(provider.getProviderType()) ? "displayName" : "name"));
        }
        if (roleClaim != null) {
            provider.setRoleClaim(roleClaim);
        }
        if (defaultRoleCodes != null) {
            provider.setDefaultRoleCodes(defaultClaim(defaultRoleCodes, "USER"));
        }
    }

    private IdentityProviderVO toVO(SsoIdentityProvider provider) {
        return IdentityProviderVO.builder()
                .id(provider.getId())
                .alias(provider.getAlias())
                .displayName(provider.getDisplayName())
                .providerType(provider.getProviderType())
                .discoveryUri(provider.getDiscoveryUri())
                .clientId(provider.getClientId())
                .scopes(provider.getScopes())
                .usernameClaim(provider.getUsernameClaim())
                .emailClaim(provider.getEmailClaim())
                .displayNameClaim(provider.getDisplayNameClaim())
                .roleClaim(provider.getRoleClaim())
                .defaultRoleCodes(provider.getDefaultRoleCodes())
                .samlEntityId(provider.getSamlEntityId())
                .samlSsoUrl(provider.getSamlSsoUrl())
                .samlMetadataUri(provider.getSamlMetadataUri())
                .enabled(provider.getEnabled())
                .loginUrl(buildLoginUrl(provider))
                .build();
    }

    private IdentityProviderVO toPublicVO(SsoIdentityProvider provider) {
        return IdentityProviderVO.builder()
                .alias(provider.getAlias())
                .displayName(provider.getDisplayName())
                .providerType(provider.getProviderType())
                .loginUrl(buildLoginUrl(provider))
                .build();
    }

    private String buildLoginUrl(SsoIdentityProvider provider) {
        if ("SAML".equalsIgnoreCase(provider.getProviderType())) {
            return ssoProperties.getIssuer() + "/saml2/authenticate/" + provider.getAlias();
        }
        return ssoProperties.getIssuer() + "/oauth2/authorization/" + provider.getAlias();
    }

    private String normalizeType(String providerType) {
        return providerType != null && "SAML".equalsIgnoreCase(providerType) ? "SAML" : "OIDC";
    }

    private String defaultClaim(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private void applySamlFields(SsoIdentityProvider provider, String metadataUri,
                                 String entityId, String ssoUrl, String certificate) {
        if (StringUtils.hasText(metadataUri)) {
            SamlMetadataVO metadata = samlMetadataParserService.parseFromUri(metadataUri);
            provider.setSamlMetadataUri(metadataUri);
            provider.setSamlEntityId(metadata.getEntityId());
            provider.setSamlSsoUrl(metadata.getSsoUrl());
            provider.setSamlVerificationCertificate(metadata.getVerificationCertificate());
            return;
        }
        provider.setSamlEntityId(entityId);
        provider.setSamlSsoUrl(ssoUrl);
        provider.setSamlVerificationCertificate(certificate);
    }
}
