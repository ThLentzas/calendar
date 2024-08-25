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
import static org.mockito.ArgumentMatchers.eq;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.datafaker.Faker;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class})
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private AuthService authService;
    private static final String AUTH_PATH = "/api/v1/auth";
    private static final Faker FAKER = new Faker();

    // registerUser()
    @Test
    void should201WhenUserIsRegisteredSuccessfully() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(FAKER.internet().username(),
                FAKER.internet().emailAddress(),
                FAKER.internet().password(12, 128, true, true, true)
        );

        doNothing().when(this.authService).registerUser(eq(registerRequest), any(HttpServletResponse.class));

        this.mockMvc.perform(post(AUTH_PATH + "/register").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        verify(this.authService, times(1)).registerUser(eq(registerRequest), any(HttpServletResponse.class));
    }

    // registerUser() Similar for username and password. Redundant to repeat
    @ParameterizedTest
    @NullAndEmptySource
    void should400WhenRegisterRequestEmailIsBlank(String email) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(FAKER.internet().username(),
                email,
                FAKER.internet().password(12, 128, true, true, true)
        );
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
                        .content(this.objectMapper.writeValueAsString(registerRequest)))
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

    // registerUser()
    @Test
    void should409WhenRegisteringUserWithExistingEmail() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(FAKER.internet().username(),
                FAKER.internet().emailAddress(),
                FAKER.internet().password(12, 128, true, true, true)
        );
        String responseBody = """
                {
                    "status": 409,
                    "type": "CONFLICT",
                    "message": "The provided email already exists",
                    "path": "/api/v1/auth/register"
                }
                """;

        doThrow(new DuplicateResourceException("The provided email already exists")).when(authService).registerUser(
                eq(registerRequest),
                any(HttpServletResponse.class));

        this.mockMvc.perform(post(AUTH_PATH + "/register").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(registerRequest)))
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
        RegisterRequest registerRequest = new RegisterRequest(FAKER.internet().username(),
                FAKER.internet().emailAddress(),
                FAKER.internet().password(12, 128, true, true, true)
        );
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
                        .content(this.objectMapper.writeValueAsString(registerRequest)))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.authService);
    }

    // registerUser()
    @Test
    void should403WhenRegisterUserIsCalledWithInvalidCsrfToken() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(FAKER.internet().username(),
                FAKER.internet().emailAddress(),
                FAKER.internet().password(12, 128, true, true, true)
        );
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
                        .content(this.objectMapper.writeValueAsString(registerRequest)))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.authService);
    }

    // refreshToken()
    @Test
    void should200WhenAccessTokenIsRefreshedSuccessfully() throws Exception {
        doNothing().when(this.authService).refresh(any(HttpServletRequest.class), any(HttpServletResponse.class));

        // We can not assert that the cookies will be present in the response because we mock the refresh(). We do that in an IT test
        this.mockMvc.perform(post(AUTH_PATH + "/token/refresh").with(csrf().asHeader())
                        .cookie(new Cookie("REFRESH_TOKEN", "value")))
                .andExpect(status().isOk());

        verify(this.authService, times(1)).refresh(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    // refreshToken()
    @Test
    void should403WhenRefreshTokenIsCalledWithNoCsrf() throws Exception {
        String responseBody = """
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "/api/v1/auth/token/refresh"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/token/refresh")
                        .cookie(new Cookie("REFRESH_TOKEN", "value")))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.authService);
    }

    // refreshToken()
    @Test
    void should403WhenRefreshTokenIsCalledWithInvalidCsrf() throws Exception {
        String responseBody = """
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "/api/v1/auth/token/refresh"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/token/refresh").with(csrf().useInvalidToken())
                        .cookie(new Cookie("REFRESH_TOKEN", "value")))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.authService);
    }

    // revokeToken()
    @Test
    void should200WhenAccessAndRefreshTokenAreRevokedSuccessfully() throws Exception {
        doNothing().when(this.authService).revoke(any(HttpServletResponse.class));

        // We can not assert that the cookies will be present in the response because we mock the refresh(). We do that in an IT test
        this.mockMvc.perform(post(AUTH_PATH + "/token/revoke").with(csrf().asHeader()))
                .andExpect(status().isOk());

        verify(this.authService, times(1)).revoke(any(HttpServletResponse.class));
    }

    // revokeToken()
    @Test
    void should403WhenRevokeTokenIsCalledWithNoCsrf() throws Exception {
        String responseBody = """
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "/api/v1/auth/token/revoke"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/token/revoke"))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.authService);
    }

    // revokeToken()
    @Test
    void should403WhenRevokeTokenIsCalledWithInvalidCsrf() throws Exception {
        String responseBody = """
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "/api/v1/auth/token/revoke"
                }
                """;

        this.mockMvc.perform(post(AUTH_PATH + "/token/revoke").with(csrf().useInvalidToken()))
                .andExpectAll(
                        status().isForbidden(),
                        content().json(responseBody, false)
                );


        verifyNoInteractions(this.authService);
    }
}
