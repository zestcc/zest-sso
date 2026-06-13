package cn.zest.sso.server.service;

import cn.zest.sso.common.constant.SsoConstants;
import cn.zest.sso.common.enums.UserStatus;
import cn.zest.sso.server.domain.entity.SsoIdentityProvider;
import cn.zest.sso.server.domain.entity.SsoRole;
import cn.zest.sso.server.domain.entity.SsoTenant;
import cn.zest.sso.server.domain.entity.SsoUser;
import cn.zest.sso.server.domain.entity.SsoUserRole;
import cn.zest.sso.server.domain.entity.SsoUserTenant;
import cn.zest.sso.server.domain.mapper.SsoIdentityProviderMapper;
import cn.zest.sso.server.domain.mapper.SsoRoleMapper;
import cn.zest.sso.server.domain.mapper.SsoTenantMapper;
import cn.zest.sso.server.domain.mapper.SsoUserMapper;
import cn.zest.sso.server.domain.mapper.SsoUserRoleMapper;
import cn.zest.sso.server.domain.mapper.SsoUserTenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IdentitySyncService {

    private final SsoUserMapper userMapper;
    private final SsoRoleMapper roleMapper;
    private final SsoTenantMapper tenantMapper;
    private final SsoUserRoleMapper userRoleMapper;
    private final SsoUserTenantMapper userTenantMapper;
    private final SsoIdentityProviderMapper identityProviderMapper;

    @Transactional(rollbackFor = Exception.class)
    public String provisionOidcUser(String registrationId, OAuth2User oauth2User) {
        SsoIdentityProvider provider = identityProviderMapper.selectOne(new LambdaQueryWrapper<SsoIdentityProvider>()
                .eq(SsoIdentityProvider::getAlias, registrationId)
                .eq(SsoIdentityProvider::getEnabled, 1));
        String username = resolveClaim(oauth2User, provider, "username", "preferred_username");
        if (!StringUtils.hasText(username)) {
            username = oauth2User.getName();
        }
        String email = resolveClaim(oauth2User, provider, "email", "email");
        String displayName = resolveClaim(oauth2User, provider, "displayName", "name");
        if (!StringUtils.hasText(displayName)) {
            displayName = username;
        }

        SsoUser existing = userMapper.selectOne(new LambdaQueryWrapper<SsoUser>().eq(SsoUser::getUsername, username));
        if (existing == null) {
            SsoUser user = new SsoUser();
            user.setUsername(username);
            user.setEmail(email);
            user.setDisplayName(displayName);
            user.setPasswordHash("{noop}FEDERATED");
            user.setStatus(UserStatus.ACTIVE.getCode());
            user.setIsSuperAdmin(0);
            user.setMfaEnabled(0);
            user.setFederationSource("OIDC:" + registrationId);
            user.setExternalId(oauth2User.getName());
            userMapper.insert(user);
            bindDefaultRoles(user.getId(), provider, oauth2User);
            bindDefaultTenant(user.getId());
            return username;
        }
        return existing.getUsername();
    }

    @Transactional(rollbackFor = Exception.class)
    public String provisionSamlUser(String registrationId, Saml2AuthenticatedPrincipal principal) {
        SsoIdentityProvider provider = identityProviderMapper.selectOne(new LambdaQueryWrapper<SsoIdentityProvider>()
                .eq(SsoIdentityProvider::getAlias, registrationId)
                .eq(SsoIdentityProvider::getProviderType, "SAML")
                .eq(SsoIdentityProvider::getEnabled, 1));
        String username = resolveSamlAttribute(principal, provider, "username", "uid");
        if (!StringUtils.hasText(username)) {
            username = principal.getName();
        }
        String email = resolveSamlAttribute(principal, provider, "email", "email");
        String displayName = resolveSamlAttribute(principal, provider, "displayName", "displayName");
        if (!StringUtils.hasText(displayName)) {
            displayName = username;
        }

        SsoUser existing = userMapper.selectOne(new LambdaQueryWrapper<SsoUser>().eq(SsoUser::getUsername, username));
        if (existing == null) {
            SsoUser user = new SsoUser();
            user.setUsername(username);
            user.setEmail(email);
            user.setDisplayName(displayName);
            user.setPasswordHash("{noop}FEDERATED");
            user.setStatus(UserStatus.ACTIVE.getCode());
            user.setIsSuperAdmin(0);
            user.setMfaEnabled(0);
            user.setFederationSource("SAML:" + registrationId);
            user.setExternalId(principal.getName());
            userMapper.insert(user);
            bindDefaultRolesFromAttributes(user.getId(), provider, principal);
            bindDefaultTenant(user.getId());
            return username;
        }
        return existing.getUsername();
    }

    private String resolveSamlAttribute(Saml2AuthenticatedPrincipal principal, SsoIdentityProvider provider,
                                        String field, String fallback) {
        String claimName = fallback;
        if (provider != null) {
            claimName = switch (field) {
                case "username" -> defaultIfBlank(provider.getUsernameClaim(), fallback);
                case "email" -> defaultIfBlank(provider.getEmailClaim(), fallback);
                case "displayName" -> defaultIfBlank(provider.getDisplayNameClaim(), fallback);
                default -> fallback;
            };
        }
        List<Object> values = principal.getAttributes().get(claimName);
        if (values != null && !values.isEmpty()) {
            return String.valueOf(values.get(0));
        }
        return null;
    }

    private void bindDefaultRolesFromAttributes(Long userId, SsoIdentityProvider provider,
                                                Saml2AuthenticatedPrincipal principal) {
        Set<String> roleCodes = new LinkedHashSet<>();
        if (provider != null && StringUtils.hasText(provider.getDefaultRoleCodes())) {
            Arrays.stream(provider.getDefaultRoleCodes().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(roleCodes::add);
        } else {
            roleCodes.add(SsoConstants.ROLE_USER);
        }
        if (provider != null && StringUtils.hasText(provider.getRoleClaim())) {
            List<Object> roles = principal.getAttributes().get(provider.getRoleClaim());
            if (roles != null) {
                roles.stream().map(String::valueOf).forEach(roleCodes::add);
            }
        }
        for (String code : roleCodes) {
            SsoRole role = roleMapper.selectOne(new LambdaQueryWrapper<SsoRole>().eq(SsoRole::getCode, code));
            if (role != null) {
                SsoUserRole ur = new SsoUserRole();
                ur.setUserId(userId);
                ur.setRoleId(role.getId());
                userRoleMapper.insert(ur);
            }
        }
    }

    private String resolveClaim(OAuth2User user, SsoIdentityProvider provider, String field, String fallback) {
        String claimName = fallback;
        if (provider != null) {
            claimName = switch (field) {
                case "username" -> defaultIfBlank(provider.getUsernameClaim(), fallback);
                case "email" -> defaultIfBlank(provider.getEmailClaim(), fallback);
                case "displayName" -> defaultIfBlank(provider.getDisplayNameClaim(), fallback);
                default -> fallback;
            };
        }
        Object value = user.getAttribute(claimName);
        return value != null ? String.valueOf(value) : null;
    }

    private void bindDefaultRoles(Long userId, SsoIdentityProvider provider, OAuth2User oauth2User) {
        Set<String> roleCodes = new LinkedHashSet<>();
        if (provider != null && StringUtils.hasText(provider.getDefaultRoleCodes())) {
            Arrays.stream(provider.getDefaultRoleCodes().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .forEach(roleCodes::add);
        } else {
            roleCodes.add(SsoConstants.ROLE_USER);
        }
        if (provider != null && StringUtils.hasText(provider.getRoleClaim())) {
            Object roles = oauth2User.getAttribute(provider.getRoleClaim());
            if (roles instanceof List<?> list) {
                list.stream().map(String::valueOf).forEach(roleCodes::add);
            } else if (roles instanceof String roleString) {
                Arrays.stream(roleString.split(",")).map(String::trim).filter(StringUtils::hasText).forEach(roleCodes::add);
            }
        }
        for (String code : roleCodes) {
            SsoRole role = roleMapper.selectOne(new LambdaQueryWrapper<SsoRole>().eq(SsoRole::getCode, code));
            if (role != null) {
                SsoUserRole ur = new SsoUserRole();
                ur.setUserId(userId);
                ur.setRoleId(role.getId());
                userRoleMapper.insert(ur);
            }
        }
    }

    private void bindDefaultTenant(Long userId) {
        SsoTenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<SsoTenant>().eq(SsoTenant::getCode, "default"));
        if (tenant != null) {
            SsoUserTenant ut = new SsoUserTenant();
            ut.setUserId(userId);
            ut.setTenantId(tenant.getId());
            ut.setIsDefault(1);
            userTenantMapper.insert(ut);
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
