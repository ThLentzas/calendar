package org.example.google_calendar_clone.role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.example.google_calendar_clone.AbstractRepositoryTest;

import static org.assertj.core.api.Assertions.assertThat;

class RoleRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private RoleRepository underTest;

    /*
        For this test the code below would result in an exception.
        IncorrectResultSizeDataAccessException: Query did not return a unique result: 2 results were returned

            Role expected = new Role();
            expected.setType(RoleType.VIEWER);
            this.underTest.save(expected);

         The reason why this happens is that since Flyway migrates our schema to the test containers, it will also execute
         the 3 INSERT statements we have to add the roles. We basically add the same roles twice by calling save().
     */
    @Test
    void shouldFindRoleByRoleType() {
        this.underTest.findByRoleType(RoleType.ROLE_VIEWER).ifPresent(actual ->
                RoleAssert.assertThat(actual).hasType(RoleType.ROLE_VIEWER));
    }

    // As mentioned above there are 3 entries from the Flyway scripts, delete them and then query.
    @Test
    void shouldReturnEmptyOptionalWhenRoleIsNotFoundByType() {
        this.underTest.deleteAll();
        assertThat(this.underTest.findByRoleType(RoleType.ROLE_VIEWER)).isEmpty();
    }
}
