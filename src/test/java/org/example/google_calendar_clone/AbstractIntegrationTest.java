package org.example.google_calendar_clone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.util.Pair;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.example.google_calendar_clone.user.UserRepository;
import org.junit.jupiter.api.AfterEach;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.redis.testcontainers.RedisContainer;

import net.datafaker.Faker;

import java.util.List;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public abstract class AbstractIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    protected static final Faker FAKER = new Faker();

    /*
        Service connections are established by using the image name of the container
        TestContainers handle the port management automatically. They dynamically assign an available port on the host
        to the container's exposed port
     */
    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15.2-alpine"))
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("google_calendar_clone_test");

    @ServiceConnection
    protected static final RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:alpine"));

    static {
        postgreSQLContainer.start();
        redisContainer.start();
    }

    @AfterEach
    void clear() {
        this.userRepository.deleteAll();
    }

    // The credentials from the users in the INIT_USERS.sql script. The import is from springframework data util.
    // It will also work with the test containers apache tuple Pair
    protected List<Pair<String, String>> userCredentials() {
        return List.of(
                Pair.of("joshua.wolf@hotmail.com", "jPt75uo0g$8_"),
                Pair.of("ericka.ankunding@hotmail.com", "kR8zV1l$5x#"),
                Pair.of("waltraud.roberts@gmail.com", "nJ2dQ4t@7y!")
        );
    }
}
