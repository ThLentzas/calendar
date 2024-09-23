package org.example.google_calendar_clone.user;

import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.DuplicateResourceException;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.user.contact.dto.CreateContactRequest;
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
    private UserRepository userRepository;
    @InjectMocks
    private UserService underTest;
    // Default is English locale
    private static final Faker FAKER = new Faker();

    // registerUser()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenUsernameExceedsMaxLength() {
        User user = createUser();
        user.setUsername(RandomStringUtils.randomAlphanumeric(21, FAKER.number().numberBetween(22, 120)));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user)).withMessage("Invalid username. Username must not exceed 20 characters");
    }

    // registerUser()
    @ParameterizedTest
    @ValueSource(strings = {"john_doe", "john@doe", "john#doe", "john doe", "john/doe", "john doe"})
    void shouldThrowIllegalArgumentExceptionWhenUsernameIsInvalid(String username) {
        User user = createUser();
        user.setUsername(username);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user)).withMessage("Invalid username. Username should contain only characters, numbers and .");
    }

    // registerUser()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailExceedsMaxLength() {
        User user = createUser();
        user.setEmail(RandomStringUtils.randomAlphanumeric(51, FAKER.number().numberBetween(52, 120)));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user)).withMessage("Invalid email. Email must not exceed 50 characters");
    }

    // registerUser()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailIsInvalid() {
        User user = createUser();
        user.setEmail("testexample.com");

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user)).withMessage("Invalid email format");
    }

    // registerUser()
    @Test
    void shouldThrowDuplicateResourceExceptionWhenRegisteringUserWithExistingEmail() {
        User user = createUser();

        when(this.userRepository.existsByEmailIgnoringCase(user.getEmail())).thenReturn(true);

        assertThatExceptionOfType(DuplicateResourceException.class).isThrownBy(() -> this.underTest.registerUser(user)).withMessage("The provided email already exists");
    }

    // findUserByIdFetchingRoles()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundById() {
        Long userId = FAKER.number().numberBetween(1L, 150L);

        when(this.userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.findById(userId)).withMessage("User not found with id: " + userId);
    }

    // addContact()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToBeAddedAsContact() {
        User sender = createUser();
        Long receiverId = FAKER.number().numberBetween(1L, 150L);
        CreateContactRequest contactRequest = new CreateContactRequest(receiverId);

        when(this.userRepository.getReferenceById(sender.getId())).thenReturn(sender);
        when(this.userRepository.findById(contactRequest.receiverId())).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.sendContactRequest(contactRequest, sender.getId())).withMessage("User not found with id: " + contactRequest.receiverId());
    }

    private User createUser() {
        // When the project was created datafaker did not support creating strings of given length, so we use RandomStringUtils
        return User.builder()
                .id(FAKER.number().numberBetween(1L, 150L))
                // Faker may generate a username with length > 20
                .username("username")
                .password(FAKER.internet().password(12, 128, true, true, true))
                .email(FAKER.internet().emailAddress())
                .build();
    }
}
