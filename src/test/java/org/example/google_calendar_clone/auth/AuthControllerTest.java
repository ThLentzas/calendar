package org.example.google_calendar_clone.auth;

import org.example.google_calendar_clone.auth.dto.RegisterRequest;
import org.example.google_calendar_clone.config.JwtConfig;
import org.example.google_calendar_clone.config.SecurityConfig;
import org.example.google_calendar_clone.exception.DuplicateResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletResponse;

import net.datafaker.Faker;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtConfig.class
})
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AuthService authService;
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final Faker FAKER = new Faker();

    // registerUser()
    @Test
    void should201WhenUserIsRegisteredSuccessfully() throws Exception {
        String username = FAKER.internet().username();
        String email = FAKER.internet().emailAddress();
        String password = FAKER.internet().password(12, 128, true, true, true);
        String requestBody = String.format("""
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """, username, email, password);
        doNothing().when(this.authService).registerUser(any(RegisterRequest.class), any(HttpServletResponse.class));

        this.mockMvc.perform(post(AUTH_PATH + "/register").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        verify(this.authService, times(1)).registerUser(any(RegisterRequest.class), any(HttpServletResponse.class));
    }

    // registerUser() Similar for username and password. Redundant to repeat
    @ParameterizedTest
    @NullAndEmptySource
    void should400WhenRegisterRequestEmailIsBlank(String email) throws Exception {
        String username = FAKER.internet().username();
        String password = FAKER.internet().password(12, 128, true, true, true);
        String emailValue = email == null ? "null" : "\"" + email + "\"";
        String requestBody = String.format("""
                {
                    "username": "%s",
                    "email": %s,
                    "password": "%s"
                }
                """, username, emailValue, password);
        String responseBody = """
                {
                    "status": 400,
                    "type": "BAD_REQUEST",
                    "message": "The email field is required",
                    "path": "/api/v1/auth/register"
                }
                """;
        this.mockMvc.perform(post(AUTH_PATH + "/register").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isBadRequest(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.authService);
    }

    @Test
    void should409WhenRegisteringUserWithExistingEmail() throws Exception{
        String username = FAKER.internet().username();
        String email = FAKER.internet().emailAddress();
        String password = FAKER.internet().password(12, 128, true, true, true);
        String requestBody = String.format("""
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """, username, email, password);
        String responseBody = """
                {
                    "status": 409,
                    "type": "CONFLICT",
                    "message": "The provided email already exists",
                    "path": "/api/v1/auth/register"
                }
                """;

        doThrow(new DuplicateResourceException("The provided email already exists")).when(authService).registerUser(
                any(RegisterRequest.class),
                any(HttpServletResponse.class));

        this.mockMvc.perform(post(AUTH_PATH + "/register").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isConflict(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );
    }

    // registerUser()
    @Test
    void should403WhenRegisterUserIsCalledWithNoCsrfToken() throws Exception {
        String username = FAKER.internet().username();
        String email = FAKER.internet().emailAddress();
        String password = FAKER.internet().password(12, 128, true, true, true);
        String requestBody = String.format("""
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """, username, email, password);
        String responseBody = """
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "/api/v1/auth/register"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.authService);
    }

    // registerUser()
    @Test
    void should403WhenRegisterUserIsCalledWithInvalidCsrfToken() throws Exception {
        String username = FAKER.internet().username();
        String email = FAKER.internet().emailAddress();
        String password = FAKER.internet().password(12, 128, true, true, true);
        String requestBody = String.format("""
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """, username, email, password);
        String responseBody = """
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "/api/v1/auth/register"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/register").with(csrf().useInvalidToken().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpectAll(
                        status().isForbidden(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.authService);
    }
}
