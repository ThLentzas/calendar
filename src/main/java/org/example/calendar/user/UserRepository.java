package org.example.calendar.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.example.calendar.entity.User;
import org.example.calendar.exception.ServerErrorException;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class UserRepository {
    private final JdbcClient jdbcClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRepository.class);

    /*
        An alternative approach of acquiring the autogenerated id

        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            user.setId(((Number) keys.get("id")).longValue());
        }
     */
    void create(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        this.jdbcClient.sql("""
                        INSERT INTO users(email, username, password)
                        VALUES (:email, :username, :password)
                        """)
                .param("email", user.getEmail())
                .param("username", user.getUsername())
                .param("password", user.getPassword())
                // If we don't pass the column that will hold the auto-generated key, all the columns will be returned and
                // when we try to access it, we would get InvalidDataAccessApiUsageException: The getKey method should only be used when a single key is returned. The current key entry contains multiple keys: [{id=5, email=sylvester.schneider@yahoo.com, username=emeline.haley, password=$2a$10$Fb0ngWuoUI.U8Q/K2.ZkZebeTM1TocDYz3Utttoe4Y80F4IhVALGm}]
                .update(keyHolder, "id");

        user.setId(keyHolder.getKeyAs(Long.class));
    }

    /*
        We use EXISTS() instead of COUNT(). It is more efficient because EXISTS stops searching once it finds a match,
        while COUNT() continues to scan all matching rows, counting them.

        We simply return a constant value (1) if the row exists. The EXISTS clause only cares about whether a row is
        found, not the actual value. The outer select returns the value that EXISTS() has.
     */
    boolean existsByEmail(String email) {
        return this.jdbcClient.sql("""
                            SELECT EXISTS(
                                SELECT 1
                                FROM users
                                WHERE email ILIKE :email)
                        """)
                .param("email", email)
                .query(Boolean.class)
                .single();
    }

    public Optional<User> findByEmail(String email) {
        return this.jdbcClient.sql("""
                            SELECT u.id, u.username, u.email, u.password
                            FROM users u
                            WHERE email ILIKE :email
                        """)
                .param("email", email)
                .query(User.class)
                .optional();
    }

    public User findAuthUserByIdOrThrow(Long id) {
        return findById(id).orElseThrow(() -> {
            LOGGER.info("Authenticated user with id: {} was not found in the database", id);
            return new ServerErrorException("Internal Server Error");
        });
    }

    Optional<User> findById(Long userId) {
        return this.jdbcClient.sql("""
                            SELECT u.id, u.username, u.email
                            FROM users u
                            WHERE id = :userId
                        """)
                .param("userId", userId)
                .query(User.class)
                .optional();
    }

    /*
        We need to wrap this method in @Transactional because it is used in @AfterEach for our IntegrationTest setup.
        If we don't wrap this with @Transactional, it will part of the transaction that runs our test(?) and since the
        transaction never commits, it will only delete the records within the transaction and every subsequent call to
        the db (SELECT, INSERT) will be invalid. It would actually return the users, because the transaction has not
        yet commited.

        https://stackoverflow.com/questions/65215214/spring-boot-integration-test-fails-if-transactional
     */
    @Transactional
    public void deleteAll() {
        this.jdbcClient.sql("""
                            DELETE
                            FROM users
                        """)
                .update();
    }

    private List<User> findAll() {
        return this.jdbcClient.sql("""
                            SELECT *
                            FROM users
                        """)
                .query(User.class)
                .list();
    }
}
