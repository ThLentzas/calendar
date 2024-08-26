package org.example.google_calendar_clone.user;

import org.example.google_calendar_clone.entity.Role;
import org.springframework.security.core.GrantedAuthority;

public record UserAuthority(Role role) implements GrantedAuthority {

    @Override
    public String getAuthority() {
        return role.getType().name();
    }
}
