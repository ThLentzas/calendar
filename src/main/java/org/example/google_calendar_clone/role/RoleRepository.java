package org.example.google_calendar_clone.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.google_calendar_clone.entity.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @Query("""
                SELECT r
                FROM Role r
                WHERE r.type = :roleType
            """)
    Optional<Role> findByRoleType(@Param("roleType") RoleType roleType);
}
