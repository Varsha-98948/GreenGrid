package com.greengrid.security;

import com.greengrid.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The authenticated principal attached to every request's SecurityContext.
 * Carries only the user id + email — controllers/services fetch the full
 * User entity themselves when they need more, keeping this lightweight
 * since it's built fresh from JWT claims on every single request.
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;

    public UserPrincipal(UUID id, String email) {
        this.id = id;
        this.email = email;
    }

    public static UserPrincipal fromUser(User user) {
        return new UserPrincipal(user.getId(), user.getEmail());
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return null; // never needed post-authentication; JWT already validated
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
