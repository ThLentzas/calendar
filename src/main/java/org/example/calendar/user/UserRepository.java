package org.example.calendar.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.calendar.entity.User;
import org.example.calendar.exception.ServerErrorException;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Logger logger = LoggerFactory.getLogger(UserRepository.class);

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

    default User findAuthUserByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> {
            logger.info("Authenticated user with id: {} was not found in the database", id);
            return new ServerErrorException("Internal Server Error");
        });
    }
}
