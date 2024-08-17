package org.example.google_calendar_clone;

import org.example.google_calendar_clone.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.redis.testcontainers.RedisContainer;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public abstract class AbstractIntegrationTest {
    @Autowired
    private UserRepository userRepository;

    /*
        Service connections are established by using the image name of the container
        TestContainers handle the port management automatically. They dynamically assign an available port on the host
        to the container's exposed port (the default PostgreSQL port, 5432, in this case
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

    @BeforeEach
    void setup() {
        this.userRepository.deleteAll();
    }
}
