package com.brightloop.subscription.security;

import com.brightloop.subscription.common.enums.Role;
import com.brightloop.subscription.feature.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts our User entity to Spring Security's UserDetails interface.
 *
 * Spring Security calls getAuthorities() to evaluate @PreAuthorize expressions.
 * hasRole('ADMIN') expects the authority "ROLE_ADMIN" (with prefix),
 * so we prefix the enum name with "ROLE_" here.
 *
 * getUsername() returns the email — it is the unique identifier used
 * as the JWT subject and for authentication lookups.
 *
 * isEnabled() delegates to user.getActive() — inactive users will cause
 * Spring Security to throw DisabledException before authentication completes.
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    /** Provides access to the raw User entity for service-layer use. */
    public User getUser() {
        return user;
    }

    public Long getUserId() {
        return user.getId();
    }

    public Role getRole() {
        return user.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    /** The JWT subject — MUST be unique per user. */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /** Delegates to user.active — Spring Security throws DisabledException if false. */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(user.getActive());
    }
}
