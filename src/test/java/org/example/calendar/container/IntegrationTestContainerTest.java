package org.example.calendar.container;

import org.example.calendar.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationTestContainerTest extends AbstractIntegrationTest {

    @Test
    void connectionEstablished() {
        assertThat(postgreSQLContainer.isCreated()).isTrue();
        assertThat(postgreSQLContainer.isRunning()).isTrue();

        assertThat(redisContainer.isCreated()).isTrue();
        assertThat(redisContainer.isRunning()).isTrue();
    }
}