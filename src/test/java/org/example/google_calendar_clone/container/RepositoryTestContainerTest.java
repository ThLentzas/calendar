package org.example.google_calendar_clone.container;

import org.example.google_calendar_clone.AbstractRepositoryTest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryTestContainerTest extends AbstractRepositoryTest {

    @Test
    void connectionEstablished() {
        assertThat(postgreSQLContainer.isCreated()).isTrue();
        assertThat(postgreSQLContainer.isRunning()).isTrue();
    }
}
