package org.example.calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.util.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.example.calendar.user.UserRepository;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.icegreen.greenmail.store.FolderException;
import com.redis.testcontainers.RedisContainer;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import net.datafaker.Faker;

import java.util.List;

import io.restassured.RestAssured;

@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "scheduling.enabled: false")
public abstract class AbstractIntegrationTest {
    @Autowired
    private UserRepository userRepository;
    // If it is declared as static it would be 0, anything static is resolved at compile time, and the port is assigned at runtime
    // https://docs.spring.io/spring-boot/api/java/org/springframework/boot/test/web/server/LocalServerPort.html
    @LocalServerPort
    protected int port;
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

    @RegisterExtension
    protected static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("test", "test"))
            .withPerMethodLifecycle(false);

    @BeforeEach
    void setup() {
        /*
            We don't have to specify the baseURI because there is a DEFAULT_URI set to "http://localhost". If we were to
            specify our own not "http://localhost:" without the colon. RestAssured will construct the URL
            RestAssured.baseURI = "http://localhost";
         */
        RestAssured.port = port;
    }

    /*
        I tried to setup in @BeforeEach but the method was getting executed after the @SQl scripts and it was deleting
        the users which made my tests fail

        Why is it better to clean in @BeforeEach()?
        https://www.youtube.com/watch?v=u5foQULTxHM 1.07.40

        We have ON DELETE CASCADE and every user related entries will also be deleted.
        We need to delete the received emails between the tests, so
            await().atMost(5, TimeUnit.SECONDS).until(() -> greenMail.getReceivedMessages().length == 1);
        does not fail, if there are previously received emails.
     */
    @AfterEach
    void clear() throws FolderException {
        // This must be @Transactional! Explained in the method itself
        this.userRepository.deleteAll();
        greenMail.purgeEmailFromAllMailboxes();
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
