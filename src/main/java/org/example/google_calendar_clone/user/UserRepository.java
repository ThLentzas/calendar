package org.example.google_calendar_clone.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.example.google_calendar_clone.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("""
               SELECT COUNT(u) > 0
               FROM User u
               WHERE LOWER(u.email) = :email
           """)
    boolean existsByEmailIgnoringCase(@Param("email") String email);

    @Query("""
               SELECT u
               FROM User u
               WHERE LOWER(u.email) = LOWER(:email)
           """)
    Optional<User> findByEmail(@Param("email") String email);
}
