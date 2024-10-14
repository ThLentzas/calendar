package org.example.calendar.user;

import org.example.calendar.entity.User;
import org.example.calendar.exception.DuplicateResourceException;
import org.example.calendar.exception.ResourceNotFoundException;
import org.example.calendar.user.contact.dto.CreateContactRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import net.datafaker.Faker;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRepository repository;
    @InjectMocks
    private UserService underTest;
    // Default is English locale
    // When the project was created datafaker did not support creating strings of given length, so we use RandomStringUtils
    private static final Faker FAKER = new Faker();

    // registerUser()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenUsernameExceedsMaxLength() {
        User user = User.builder()
                .username(RandomStringUtils.randomAlphanumeric(21, FAKER.number().numberBetween(22, 120)))
                .build();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user)).withMessage("Invalid username. Username must not exceed 20 characters");
    }

    // registerUser()
    @ParameterizedTest
    @ValueSource(strings = {"john_doe", "john@doe", "john#doe", "john doe", "john/doe", "john doe"})
    void shouldThrowIllegalArgumentExceptionWhenUsernameIsInvalid(String username) {
        User user = User.builder()
                .username(username)
                .build();

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user)).withMessage("Invalid username. Username should contain only characters, numbers and .");
    }

    // registerUser()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailExceedsMaxLength() {
        User user = User.builder()
                // Faker may generate a username with length > 20
                .username("username")
                .email(RandomStringUtils.randomAlphanumeric(51, FAKER.number().numberBetween(52, 120)))
                .build();

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user)).withMessage("Invalid email. Email must not exceed 50 characters");
    }

    // registerUser()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailIsInvalid() {
        User user = User.builder()
                .username("username")
                .email("testexample.com")
                .build();

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user)).withMessage("Invalid email format");
    }

    // registerUser()
    @Test
    void shouldThrowDuplicateResourceExceptionWhenRegisteringUserWithExistingEmail() {
        User user = User.builder()
                .username("username")
                .email(FAKER.internet().emailAddress())
                .password(FAKER.internet().password(12, 128, true, true, true))
                .build();

        when(this.repository.existsByEmail(user.getEmail())).thenReturn(true);

        assertThatExceptionOfType(DuplicateResourceException.class).isThrownBy(() -> this.underTest.registerUser(user)).withMessage("The provided email already exists");
    }

    // findUserByIdFetchingRoles()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundById() {
        Long userId = 1L;

        when(this.repository.findById(userId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.findById(userId)).withMessage("User not found with id: " + userId);
    }

    // addContact()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToBeAddedAsContact() {
        Long senderId = 1L;
        Long receiverId = 2L;
        CreateContactRequest contactRequest = new CreateContactRequest(receiverId);

        when(this.repository.findById(contactRequest.receiverId())).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.sendContactRequest(contactRequest, senderId)).withMessage("User not found with id: " + contactRequest.receiverId());
    }
}
