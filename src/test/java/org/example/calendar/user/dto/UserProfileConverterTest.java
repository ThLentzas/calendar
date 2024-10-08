package org.example.calendar.user.dto;

import org.example.calendar.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import net.datafaker.Faker;

class UserProfileConverterTest {
    private final UserProfileConverter profileConverter = new UserProfileConverter();
    private static final Faker FAKER = new Faker();

    @Test
    void shouldConvertUserToUserProfile() {
        Long userId = FAKER.number().numberBetween(1L, 1000L);
        String username = FAKER.internet().username();
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setPassword(FAKER.internet().password(12, 128, true, true, true));
        UserProfile expected = new UserProfile(userId, username);

        UserProfile actual = profileConverter.convert(user);

        assertThat(actual).isEqualTo(expected);
    }
}
