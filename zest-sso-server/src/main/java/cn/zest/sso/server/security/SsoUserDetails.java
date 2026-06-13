package cn.zest.sso.server.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SSO 用户认证详情，线程安全由 Spring Security 管理。
 */
@Getter
public class SsoUserDetails implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String password;
    private final String email;
    private final String displayName;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final boolean superAdmin;
    private final List<String> roles;
    private final List<String> groups;
    private final Long defaultTenantId;
    private final String defaultTenantCode;
    private final Collection<? extends GrantedAuthority> authorities;

    public SsoUserDetails(Long userId, String username, String password, String email,
                          String displayName, boolean enabled, boolean accountNonLocked,
                          boolean superAdmin, List<String> roles, List<String> groups,
                          Long defaultTenantId, String defaultTenantCode) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.displayName = displayName;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.superAdmin = superAdmin;
        this.roles = roles;
        this.groups = groups;
        this.defaultTenantId = defaultTenantId;
        this.defaultTenantCode = defaultTenantCode;
        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
