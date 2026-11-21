package com.notificationapp.service;

import com.notificationapp.model.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The logged-in user. Carries the user id, so ownership checks can compare it with the id in a request
 * ({@code authentication.principal.id}).
 */
@Getter
public class AuthenticatedUser implements UserDetails {

    private final UUID id;
    private final String username;
    private final String password;
    private final List<GrantedAuthority> authorities;

    private AuthenticatedUser(UUID id, String username, String password, List<GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

}
