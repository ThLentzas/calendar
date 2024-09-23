package org.example.google_calendar_clone.user.contact;

import org.example.google_calendar_clone.AbstractRepositoryTest;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.user.UserProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
    IMPORTANT!!!!!! This class uses sql scripts for setting up data. The ContactRequestRepositoryTest and the UserRepositoryTest
    classes use set up methods.
 */
class ContactRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private ContactRepository underTest;

    /*
        From the sql scripts there is a record in the contacts table as INSERT INTO contacts VALUES (1, 3);. We create
        a user based on the values of user with id 3 in the scripts.
     */
    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_CONTACT_REQUESTS.sql"})
    void shouldFindContacts() {
        User expected = createUser();
        List<UserProjection> projections = this.underTest.findContacts(1L);

        assertThat(projections).anyMatch(userProjection -> userProjection.getId().equals(expected.getId()) && userProjection.getUsername().equals(expected.getUsername()));
    }

    private User createUser() {
        User user = new User();
        user.setId(3L);
        user.setUsername("ellyn.roberts");
        user.setEmail("waltraud.roberts@gmail.com");

        return user;
    }
}
