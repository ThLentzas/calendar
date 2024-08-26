package org.example.google_calendar_clone.user;

import org.example.google_calendar_clone.entity.Role;
import org.example.google_calendar_clone.entity.User;
import org.example.google_calendar_clone.exception.DuplicateResourceException;
import org.example.google_calendar_clone.exception.ResourceNotFoundException;
import org.example.google_calendar_clone.role.RoleType;
import org.example.google_calendar_clone.user.contact.dto.CreateContactRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
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
        user.setUsername(RandomStringUtils.randomAlphanumeric(21, FAKER.number().numberBetween(22, 120)));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user))
                .withMessage("Invalid username. Username must not exceed 20 characters");
    }

    // registerUser()
    @ParameterizedTest
    @ValueSource(strings = {"john_doe", "john@doe", "john#doe", "john doe", "john/doe", "john doe"})
    void shouldThrowIllegalArgumentExceptionWhenUsernameIsInvalid(String username) {
        User user = createUser();
        user.setUsername(username);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> this.underTest.registerUser(user))
                .withMessage("Invalid username. Username should contain only characters, numbers and .");
    }

    // registerUser()
    @Test
    void shouldThrowIllegalArgumentExceptionWhenEmailExceedsMaxLength() {
        User user = createUser();
        user.setEmail(RandomStringUtils.randomAlphanumeric(51, FAKER.number().numberBetween(52, 120)));

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
        Long userId = FAKER.number().numberBetween(1L, 150L);

        when(this.userRepository.findByIdFetchingRoles(userId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.findByIdFetchingRoles(userId))
                .withMessage("User not found with id: " + userId);
    }

    // addContact()
    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserIsNotFoundToBeAddedAsContact() {
        User sender = createUser();
        Long receiverId = FAKER.number().numberBetween(1L, 150L);
        CreateContactRequest contactRequest = new CreateContactRequest(receiverId);
        Jwt mockJwt = mock(Jwt.class);

        when(mockJwt.getSubject()).thenReturn(String.valueOf(sender.getId()));
        when(this.userRepository.getReferenceById(sender.getId())).thenReturn(sender);
        when(this.userRepository.findById(contactRequest.receiverId())).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> this.underTest.sendContactRequest(
                        contactRequest,
                        mockJwt))
                .withMessage("User not found with id: " + contactRequest.receiverId());
    }

    private User createUser() {
        Role role = new Role(RoleType.ROLE_VIEWER);
        // When the project was created datafaker did not support creating strings of given length, so we use RandomStringUtils
        User user = new User("username",
                FAKER.internet().password(12, 128, true, true, true),
                FAKER.internet().emailAddress(),
                Set.of(role));
        user.setId(FAKER.number().numberBetween(1L, 150L));

        return user;
    }
}
