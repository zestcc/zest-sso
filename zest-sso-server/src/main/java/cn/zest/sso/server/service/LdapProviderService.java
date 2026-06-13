package cn.zest.sso.server.service;

import cn.zest.sso.common.enums.AuditEventType;
import cn.zest.sso.common.exception.ErrorCode;
import cn.zest.sso.common.exception.SsoException;
import cn.zest.sso.server.domain.dto.CreateLdapProviderRequest;
import cn.zest.sso.server.domain.dto.UpdateLdapProviderRequest;
import cn.zest.sso.server.domain.entity.SsoLdapProvider;
import cn.zest.sso.server.domain.mapper.SsoLdapProviderMapper;
import cn.zest.sso.server.domain.vo.LdapProviderVO;
import cn.zest.sso.server.support.AdminAuditSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LdapProviderService {

    private final SsoLdapProviderMapper ldapProviderMapper;
    private final AdminAuditSupport auditSupport;

    public Page<LdapProviderVO> pageProviders(int page, int size) {
        Page<SsoLdapProvider> result = ldapProviderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SsoLdapProvider>().orderByAsc(SsoLdapProvider::getAlias));
        Page<LdapProviderVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    public LdapProviderVO getById(Long id) {
        return toVO(findById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public LdapProviderVO create(CreateLdapProviderRequest request) {
        assertAliasUnique(request.getAlias(), null);
        SsoLdapProvider provider = new SsoLdapProvider();
        applyCreate(provider, request);
        provider.setEnabled(1);
        ldapProviderMapper.insert(provider);
        auditSupport.log(AuditEventType.LDAP_CREATE, provider.getAlias(), provider.getDisplayName());
        return toVO(provider);
    }

    @Transactional(rollbackFor = Exception.class)
    public LdapProviderVO update(Long id, UpdateLdapProviderRequest request) {
        SsoLdapProvider provider = findById(id);
        applyUpdate(provider, request);
        ldapProviderMapper.updateById(provider);
        auditSupport.log(AuditEventType.LDAP_UPDATE, provider.getAlias(), provider.getDisplayName());
        return toVO(ldapProviderMapper.selectById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SsoLdapProvider provider = findById(id);
        ldapProviderMapper.deleteById(id);
        auditSupport.log(AuditEventType.LDAP_DELETE, provider.getAlias(), provider.getDisplayName());
    }

    public void testConnection(Long id) {
        SsoLdapProvider provider = findById(id);
        LdapContextSource contextSource = buildContextSource(provider);
        contextSource.afterPropertiesSet();
        contextSource.getContext(provider.getBindDn(), provider.getBindPassword());
    }

    public List<LdapAuthenticationProvider> buildAuthenticationProviders() {
        List<SsoLdapProvider> providers = ldapProviderMapper.selectList(new LambdaQueryWrapper<SsoLdapProvider>()
                .eq(SsoLdapProvider::getEnabled, 1));
        List<LdapAuthenticationProvider> result = new ArrayList<>();
        for (SsoLdapProvider provider : providers) {
            result.add(buildProvider(provider));
        }
        return result;
    }

    private LdapAuthenticationProvider buildProvider(SsoLdapProvider provider) {
        LdapContextSource contextSource = buildContextSource(provider);
        contextSource.afterPropertiesSet();

        FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(
                provider.getUserSearchBase(),
                provider.getUserSearchFilter() != null ? provider.getUserSearchFilter() : "(uid={0})",
                contextSource);

        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(userSearch);

        DefaultLdapAuthoritiesPopulator authoritiesPopulator = null;
        if (StringUtils.hasText(provider.getGroupSearchBase())) {
            authoritiesPopulator = new DefaultLdapAuthoritiesPopulator(
                    contextSource, provider.getGroupSearchBase());
            if (StringUtils.hasText(provider.getGroupRoleAttribute())) {
                authoritiesPopulator.setGroupRoleAttribute(provider.getGroupRoleAttribute());
            }
        }

        return authoritiesPopulator != null
                ? new LdapAuthenticationProvider(authenticator, authoritiesPopulator)
                : new LdapAuthenticationProvider(authenticator);
    }

    private LdapContextSource buildContextSource(SsoLdapProvider provider) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(provider.getServerUrl());
        contextSource.setBase(provider.getBaseDn());
        if (StringUtils.hasText(provider.getBindDn())) {
            contextSource.setUserDn(provider.getBindDn());
            contextSource.setPassword(provider.getBindPassword());
        }
        return contextSource;
    }

    private void applyCreate(SsoLdapProvider provider, CreateLdapProviderRequest request) {
        provider.setAlias(request.getAlias());
        provider.setDisplayName(request.getDisplayName());
        provider.setServerUrl(request.getServerUrl());
        provider.setBaseDn(request.getBaseDn());
        provider.setBindDn(request.getBindDn());
        provider.setBindPassword(request.getBindPassword());
        provider.setUserSearchBase(request.getUserSearchBase());
        provider.setUserSearchFilter(StringUtils.hasText(request.getUserSearchFilter())
                ? request.getUserSearchFilter() : "(uid={0})");
        provider.setGroupSearchBase(request.getGroupSearchBase());
        provider.setGroupRoleAttribute(StringUtils.hasText(request.getGroupRoleAttribute())
                ? request.getGroupRoleAttribute() : "cn");
    }

    private void applyUpdate(SsoLdapProvider provider, UpdateLdapProviderRequest request) {
        if (request.getDisplayName() != null) provider.setDisplayName(request.getDisplayName());
        if (request.getServerUrl() != null) provider.setServerUrl(request.getServerUrl());
        if (request.getBaseDn() != null) provider.setBaseDn(request.getBaseDn());
        if (request.getBindDn() != null) provider.setBindDn(request.getBindDn());
        if (request.getBindPassword() != null) provider.setBindPassword(request.getBindPassword());
        if (request.getUserSearchBase() != null) provider.setUserSearchBase(request.getUserSearchBase());
        if (request.getUserSearchFilter() != null) provider.setUserSearchFilter(request.getUserSearchFilter());
        if (request.getGroupSearchBase() != null) provider.setGroupSearchBase(request.getGroupSearchBase());
        if (request.getGroupRoleAttribute() != null) provider.setGroupRoleAttribute(request.getGroupRoleAttribute());
        if (request.getEnabled() != null) provider.setEnabled(request.getEnabled());
    }

    private void assertAliasUnique(String alias, Long excludeId) {
        LambdaQueryWrapper<SsoLdapProvider> wrapper = new LambdaQueryWrapper<SsoLdapProvider>()
                .eq(SsoLdapProvider::getAlias, alias);
        if (excludeId != null) {
            wrapper.ne(SsoLdapProvider::getId, excludeId);
        }
        if (ldapProviderMapper.selectCount(wrapper) > 0) {
            throw new SsoException(ErrorCode.CONFLICT, "LDAP 别名已存在");
        }
    }

    private SsoLdapProvider findById(Long id) {
        SsoLdapProvider provider = ldapProviderMapper.selectById(id);
        if (provider == null) {
            throw new SsoException(ErrorCode.NOT_FOUND, "LDAP 配置不存在");
        }
        return provider;
    }

    private LdapProviderVO toVO(SsoLdapProvider provider) {
        return LdapProviderVO.builder()
                .id(provider.getId())
                .alias(provider.getAlias())
                .displayName(provider.getDisplayName())
                .serverUrl(provider.getServerUrl())
                .baseDn(provider.getBaseDn())
                .bindDn(provider.getBindDn())
                .userSearchBase(provider.getUserSearchBase())
                .userSearchFilter(provider.getUserSearchFilter())
                .groupSearchBase(provider.getGroupSearchBase())
                .groupRoleAttribute(provider.getGroupRoleAttribute())
                .enabled(provider.getEnabled())
                .build();
    }
}
