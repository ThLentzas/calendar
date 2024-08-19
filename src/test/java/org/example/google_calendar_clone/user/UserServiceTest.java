package org.example.google_calendar_clone.user;

import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.DuplicateResourceException;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import net.datafaker.Faker;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService underTest;
    // Default is English locale
    private static final Faker FAKER = new Faker();

    // registerUser()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenUsernameExceedsMaxLength() {
        User user = createUser();
        user.setUsername(RandomStringUtils.randomAlphanumeric(21, RandomUtils.nextInt(22, 120)));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user))
                .withMessage("Invalid username. Username must not exceed 20 characters");
    }

    // registerUser()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailExceedsMaxLength() {
        User user = createUser();
        user.setEmail(RandomStringUtils.randomAlphanumeric(51, RandomUtils.nextInt(52, 120)));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user))
                .withMessage("Invalid email. Email must not exceed 50 characters");
    }

    // registerUser()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailIsInvalid() {
        User user = createUser();
        user.setEmail("testexample.com");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user))
                .withMessage("Invalid email format");
    }

    // registerUser()
    @Test
    void shouldThrowDuplicateResourceExceptionWhenRegisteringUserWithExistingEmail() {
        User user = createUser();

        when(this.userRepository.existsByEmailIgnoringCase(user.getEmail())).thenReturn(true);

        assertThatExceptionOfType(DuplicateResourceException.class).isThrownBy(() -> this.underTest.registerUser(user))
                .withMessage("The provided email already exists");
    }

    // findUserByIdFetchingRoles()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundById() {
        Long userId = 1L;

        when(this.userRepository.findByIdFetchingRoles(userId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.findByIdFetchingRoles(userId))
                .withMessage("User not found with id: " + userId);
    }


    private User createUser() {
        // When the project was created datafaker did not support creating strings of given length, so we use RandomStringUtils
        return new User(FAKER.internet().username(),
                FAKER.internet().password(12, 128, true, true, true),
                FAKER.internet().emailAddress(),
                new HashSet<>());
    }
}
