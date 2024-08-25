package org.example.google_calendar_clone.user;

import org.example.google_calendar_clone.AbstractRepositoryTest;
import org.example.google_calendar_clone.entity.Role;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.role.RoleRepository;
import org.example.google_calendar_clone.role.RoleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.junit.jupiter.api.Test;

import java.util.Set;

import net.datafaker.Faker;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private UserRepository underTest;
    @Autowired
    private RoleRepository roleRepository;
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
        User actual = createUser();
        assertThat(this.underTest.existsByEmailIgnoringCase(actual.getEmail())).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotExistsWithEmailIgnoringCase() {
        assertThat(this.underTest.existsByEmailIgnoringCase(FAKER.internet().emailAddress())).isFalse();
    }

    @Test
    void shouldFindUserByIdFetchingRoles() {
        User expected = createUser();
        this.underTest.findByIdFetchingRoles(expected.getId()).ifPresent(actual ->
                // We have Assertj assertThat() and it would cause conflict if we try to static import
                UserAssert.assertThat(actual).hasUsername(expected.getUsername())
                        .hasPassword(expected.getPassword())
                        .hasEmailIgnoringCase(expected.getEmail())
                        .hasRoles(expected.getRoles()));
    }

    /*
        This method is getting called from @Test methods that are wrapped with @Transactional and will overwrite as
        mentioned above their @Transactional. We use the this.testEntityManager.flush() to flush the changes and, then
        we roll back the entire transaction when the method exits. To test that, we hard code the value of the setupUser()
        to email.com. If the changes did not roll back, the print statement since it gets executed 2nd it should return
        true but, it does not.

            @Test
            @Order(1)
            void shouldReturnTrueWhenUserExistsWithEmailIgnoringCase() {
                User actual = setupUser();
                assertThat(this.underTest.existsByEmailIgnoringCase(actual.getEmail())).isTrue();
            }

            @Test
            @Order(2)
            void shouldFindUserByIdFetchingRoles() {
                System.out.println(this.underTest.existsByEmailIgnoringCase("email.com"));
                User expected = setupUser();
                this.underTest.findByIdFetchingRoles(expected.getId()).ifPresent(actual ->
                        UserAssert.assertThat(actual).hasUsername(expected.getUsername())
                                .hasPassword(expected.getPassword())
                                .hasEmailIgnoringCase(expected.getEmail())
                                .hasRoles(expected.getRoles()));
            }
     */
    private User createUser() {
        Role role = new Role(RoleType.ROLE_VIEWER);
        this.roleRepository.save(role);
        User user = new User(FAKER.internet().username(),
                FAKER.internet().password(12, 128, true, true, true),
                FAKER.internet().emailAddress(),
                Set.of(role));
        this.underTest.save(user);
        this.testEntityManager.flush();

        return user;
    }
}
