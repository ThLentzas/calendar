package org.example.calendar.user;

import org.example.calendar.AbstractRepositoryTest;
import org.example.calendar.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;

import static org.assertj.core.api.Assertions.assertThat;

/*
    IMPORTANT!!!!!! This class and the ContactRequestRepositoryTest class use methods for setting up data. The
    ContactRepositoryTest uses sql scripts.
 */
class UserRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private UserRepository underTest;
    @Autowired
    private TestEntityManager testEntityManager;
    // https://www.datafaker.net/documentation/providers/
    // Default is English locale
    private static final Faker FAKER = new Faker();

    /*
        Why the bellow test does not throw TransientObjectException?
            @Test
            void test() {
                User actual = new User("user", "password", "test user", Set.of(new Role(RoleType.VIEWER)));
                this.underTest.save(actual);

                System.out.println(this.underTest.existsByEmailIgnoringCase(actual.getEmail()));
            }
         The reason is that since this is @DataJpaTest, the method is wrapped with @Transactional annotation, meaning
         the changes will be commited at the end of the test method. When we call this.underTest.save(actual); we would
         expect to throw an Exception since Hibernate is not aware of the Role Entity, but a TransientObjectException is
         only thrown when we flush the changes into the database. Since our method is Transactional from the @DataJpaTest
         it overwrites the @Transactional from the save(), meaning save only persists the user into the context and
         no changes are flushed into the database. When we call this.underTest.existsByEmailIgnoringCase(actual.getEmail())
         it looks at the Context and retrieves the user from there (first level cache). To solve the above issue we
         need to flush the changes after calling save with a proper setup

                    Role role = new Role(RoleType.VIEWER);
                    this.roleRepository.save(role);
                    User expected = new User(faker.name().name(),
                            this.faker.internet().password(12, 128, true, true, true),
                            this.faker.internet().emailAddress(),
                            Set.of(role));
                    this.underTest.save(expected);
                    this.testEntityManager.flush();

        https://github.com/spring-projects/spring-boot/issues/11183
        https://stackoverflow.com/questions/64459905/persist-many-to-many-relationship-with-datajpatest-and-h2-database-not-working
        https://stackoverflow.com/questions/72020788/field-annotated-with-transient-being-persisted-in-datajpatest
     */

    @Test
    void shouldReturnTrueWhenUserExistsWithEmailIgnoringCase() {
        User user = User.builder()
                .username(FAKER.internet().username())
                .password(FAKER.internet().password(12, 128, true, true, true))
                .email(FAKER.internet().emailAddress())
                .build();
        this.underTest.save(user);
        this.testEntityManager.flush();

        assertThat(this.underTest.existsByEmailIgnoringCase(user.getEmail())).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotExistsWithEmailIgnoringCase() {
        assertThat(this.underTest.existsByEmailIgnoringCase(FAKER.internet().emailAddress())).isFalse();
    }
}
