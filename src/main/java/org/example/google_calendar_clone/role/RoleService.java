package org.example.google_calendar_clone.role;

import org.example.google_calendar_clone.entity.Role;
import org.springframework.stereotype.Service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    public Optional<Role> findByRoleType(RoleType roleType) {
        return this.roleRepository.findByRoleType(roleType);
    }
}
