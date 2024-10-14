package org.example.calendar.user;

import org.example.calendar.AbstractRepositoryTest;
import org.example.calendar.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import net.datafaker.Faker;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(UserRepository.class)
class UserRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private UserRepository underTest;
    // https://www.datafaker.net/documentation/providers/
    // Default is English locale
    private static final Faker FAKER = new Faker();

    @Test
    void shouldCreateUser() {
        User user = User.builder()
                .username(FAKER.internet().username())
                .password(FAKER.internet().password(12, 128, true, true, true))
                .email(FAKER.internet().emailAddress())
                .build();
        this.underTest.create(user);

        UserAssert.assertThat(user)
                .hasId()
                .hasUsername(user.getUsername())
                .hasEmail(user.getEmail());
    }

    @Test
    void shouldReturnTrueWhenUserExistsWithEmail() {
        User user = User.builder()
                .username(FAKER.internet().username())
                .password(FAKER.internet().password(12, 128, true, true, true))
                .email(FAKER.internet().emailAddress())
                .build();
        this.underTest.create(user);

        assertThat(this.underTest.existsByEmail(user.getEmail())).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotExistsWithEmail() {
        assertThat(this.underTest.existsByEmail(FAKER.internet().emailAddress())).isFalse();
    }

    @Test
    void shouldFindUserByEmail() {
        this.underTest.findByEmail("delois.abshire@hotmail.com").ifPresent(user ->
                UserAssert.assertThat(user)
                        .hasIdValue(4L)
                        .hasUsername("silas.stracke")
                        .hasEmail("delois.abshire@hotmail.com"));
    }

    @Test
    void shouldReturnEmptyOptionalWhenUserIsNotFoundByEmail() {
        assertThat(this.underTest.findByEmail("test@example.com")).isEmpty();
    }

    @Test
    void shouldFindUserById() {
        this.underTest.findById(4L).ifPresent(user ->
                UserAssert.assertThat(user)
                        .hasIdValue(4L)
                        .hasUsername("silas.stracke")
                        .hasEmail("delois.abshire@hotmail.com"));
    }

    @Test
    void shouldReturnEmptyOptionalWhenUserIsNotFoundById() {
        assertThat(this.underTest.findById(5L)).isEmpty();
    }
}
