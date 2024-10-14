package org.example.calendar.user.contact;

import org.example.calendar.AbstractRepositoryTest;
import org.example.calendar.entity.Contact;
import org.example.calendar.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
    According to the docs, @JdbcTest does not include any @Component beans as part of our application context in our
    slice testing. In order to have access to the jdbcClient that our ContactRepository depends on, we import it and
    autowire it. Spring will see that is a bean and satisfy its dependencies.

    Don't try to autowire the jdbcClient and pass it to the repository. It will not work, and also it defeats the whole
    concept of letting Spring satisfy the dependencies of the beans

    https://docs.spring.io/spring-boot/docs/2.1.2.RELEASE/reference/html/boot-features-testing.html
 */
@Import(ContactRepository.class)
class ContactRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private ContactRepository underTest;

    @Test
    @Sql({"/scripts/INIT_USERS.sql", "/scripts/INIT_CONTACT_REQUESTS.sql"})
    void shouldCreateContact() {
        Contact contact = new Contact(1L, 3L);
        this.underTest.create(contact);

        // findContacts() is also tested indirectly
        List<User> users = this.underTest.findContacts(1L);

        assertThat(users).anyMatch(user -> user.getId().equals(3L)
                && user.getUsername().equals("ellyn.roberts")
                && user.getEmail().equals("waltraud.roberts@gmail.com"));
    }
}
