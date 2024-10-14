package org.example.calendar;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/*
    Why we don't use @TestContainers and @Container?

    @TestContainer is an annotation for Junit to look at this class for any defined @Containers. We don't let junit
    handle the lifecycle of our containers because we don't want them to be tied to the lifecycle of the class. We want
    all our repository tests to run against the same database, and we provide a clean state each time, since the
    transaction is rolled back by @JdbcTest.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractRepositoryTest {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15.2-alpine"))
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("google_calendar_clone_test");

    static {
        postgreSQLContainer.start();
    }
}