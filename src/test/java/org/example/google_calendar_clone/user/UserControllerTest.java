package org.example.google_calendar_clone.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.example.google_calendar_clone.config.SecurityConfig;
import org.example.google_calendar_clone.user.contact.dto.CreateContactRequest;
import org.example.google_calendar_clone.user.contact.request.ContactRequestAction;
import org.example.google_calendar_clone.user.contact.request.ContactRequestStatus;
import org.example.google_calendar_clone.user.contact.dto.PendingContactRequest;
import org.example.google_calendar_clone.user.contact.dto.UpdateContactRequest;
import org.example.google_calendar_clone.user.dto.UserProfile;
import org.example.google_calendar_clone.utils.AuthUtils;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import net.datafaker.Faker;

import com.fasterxml.jackson.databind.ObjectMapper;

/*
    We don't need to use @WithMockUser(), the authentication is of type JwtAuthenticationToken and is handled by
    the with(authentication())
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private JwtDecoder jwtDecoder;
    @MockBean
    private UserService userService;
    private static final String USER_PATH = "/api/v1/user";
    private static final Faker FAKER = new Faker();

    /*
        We could pass our own custom security context with a Jwt authentication like the below approach

        public class TestSecurityContextFactory implements WithSecurityContextFactory<WithCustomMockUser> {

            @Override
            public SecurityContext createSecurityContext(WithCustomMockUser annotation) {
                SecurityContext testSecurityContext = SecurityContextHolder.createEmptyContext();
                String tokenValue = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJzZWxmIiwic3ViIjoiMSIsImV4cCI6MTcyNDUzNjYzM...";
                Instant iat = Instant.now();
                Instant exp = Instant.now().plusSeconds(300L);

                Map<String, Object> headers = new LinkedHashMap<>();
                headers.put("alg", "RS256");
                headers.put("type", "JWT");
                Map<String, Object> claims = new LinkedHashMap<>();
                claims.put("iss", "self");
                // userId
                claims.put("sub", "1");
                claims.put("iat", iat.getEpochSecond());
                claims.put("exp", exp.getEpochSecond());
                claims.put("authorities", "ROLE_" + annotation.roles()[0]);

                List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("ROLE_" + annotation.roles()[0]);
                Jwt jwt = new Jwt(tokenValue, iat, exp, headers, claims);

                Authentication authentication = new JwtAuthenticationToken(jwt, authorities);
                testSecurityContext.setAuthentication(authentication);

                return testSecurityContext;
            }

            @Retention(RetentionPolicy.RUNTIME)
            @WithSecurityContext(factory = TestSecurityContextFactory.class)
            public @interface WithCustomMockUser {
                String username() default "user";
                String password() default "password";
                String[] roles();
            }

            @Test
            @WithCustomMockUser(roles = "MEMBER")
            void should200WhenAddContactIsSuccessful() throws Exception {
                CreateContactRequest contactRequest = new CreateContactRequest(FAKER.number().numberBetween(1L, 1000L));

                doNothing().when(this.userService).addContact(eq(contactRequest), any(Jwt.class));

                this.mockMvc.perform(post(USER_PATH + "/contacts").with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(this.objectMapper.writeValueAsString(contactRequest)))
                        .andExpect(status().isOk());

                verify(this.userService, times(1)).addContact(eq(contactRequest), any(Jwt.class));
            }

        sendContactRequest()
     */
    @Test
    void should200WhenSendContactRequestIsSuccessful() throws Exception {
        CreateContactRequest contactRequest = new CreateContactRequest(FAKER.number().numberBetween(1L, 1000L));

        doNothing().when(this.userService).sendContactRequest(eq(contactRequest), any(Jwt.class));

        this.mockMvc.perform(post(USER_PATH + "/contacts").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(contactRequest))
                        /*
                            For custom claims https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#_jwt_requestpostprocessor

                            Passing just a jwt like .with(jwt().jwt(jwt))); will not work because the Authorization happens based on the
                            Granted Authorities of the authentication not the jwt, but the JwtAuthenticationToken
                         */
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpect(status().isOk());

        verify(this.userService, times(1)).sendContactRequest(eq(contactRequest), any(Jwt.class));
    }

    // sendContactRequest() @Valid
    @Test
    void should400WhenCreateContactRequestIdIsNull() throws Exception {
        CreateContactRequest contactRequest = new CreateContactRequest(null);
        String responseBody = """
                {
                    "status": 400,
                    "type": "BAD_REQUEST",
                    "message": "You must provide the id of the receiver",
                    "path": "/api/v1/user/contacts"
                }
                """;

        this.mockMvc.perform(post(USER_PATH + "/contacts").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(contactRequest))
                        /*
                            For custom claims https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#_jwt_requestpostprocessor

                            Passing just a jwt like .with(jwt().jwt(jwt))); will not work because the Authorization happens based on the
                            Granted Authorities of the authentication not the jwt, but the JwtAuthenticationToken
                         */
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isBadRequest(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.userService);
    }

    // sendContactRequest()
    @Test
    void should401WhenSendContactRequestIsCalledByUnauthenticatedUser() throws Exception {
        CreateContactRequest contactRequest = new CreateContactRequest(FAKER.number().numberBetween(1L, 1000L));
        String responseBody = """
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "/api/v1/user/contacts"
                }
                """;

        this.mockMvc.perform(post(USER_PATH + "/contacts").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(contactRequest)))
                .andExpectAll(
                        status().isUnauthorized(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.userService);
    }

    // sendContactRequest()
    @Test
    void should403WhenSendContactRequestIsCalledWithNoCsrf() throws Exception {
        CreateContactRequest contactRequest = new CreateContactRequest(FAKER.number().numberBetween(1L, 1000L));
        String responseBody = """
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "/api/v1/user/contacts"
                }
                """;

        this.mockMvc.perform(post(USER_PATH + "/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(contactRequest))
                        /*
                            For custom claims https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#_jwt_requestpostprocessor

                            Passing just a jwt like .with(jwt().jwt(jwt))); will not work because the Authorization happens based on the
                            Granted Authorities of the authentication not the jwt, but the JwtAuthenticationToken
                         */
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.userService);
    }

    // sendContactRequest()
    @Test
    void should403WhenSendContactRequestIsCalledWithInvalidCsrf() throws Exception {
        CreateContactRequest contactRequest = new CreateContactRequest(FAKER.number().numberBetween(1L, 1000L));
        String responseBody = """
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "/api/v1/user/contacts"
                }
                """;

        this.mockMvc.perform(post(USER_PATH + "/contacts").with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(contactRequest))
                        /*
                            For custom claims https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#_jwt_requestpostprocessor

                            Passing just a jwt like .with(jwt().jwt(jwt))); will not work because the Authorization happens based on the
                            Granted Authorities of the authentication not the jwt, but the JwtAuthenticationToken
                         */
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                         /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.userService);
    }

    // findPendingContactsRequests()
    @Test
    void should200WithListOfPendingContactRequests() throws Exception {
        PendingContactRequest request1 = new PendingContactRequest(
                new UserProfile(FAKER.number().numberBetween(1L, 1000L), FAKER.internet().username()),
                ContactRequestStatus.PENDING
        );
        PendingContactRequest request2 = new PendingContactRequest(
                new UserProfile(FAKER.number().numberBetween(1L, 1000L), FAKER.internet().username()),
                ContactRequestStatus.PENDING
        );

        when(this.userService.findPendingContactsRequests(any(Jwt.class))).thenReturn(List.of(request1, request2));

        this.mockMvc.perform(get(USER_PATH + "/contact-requests")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isOk(),
                        content().json(this.objectMapper.writeValueAsString(List.of(request1, request2)))
                );
    }


    // findPendingContactsRequests()
    @Test
    void should401WhenFindPendingContactRequestsIsCalledByUnauthenticatedUser() throws Exception {
        String responseBody = """
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "/api/v1/user/contact-requests"
                }
                """;

        this.mockMvc.perform(get(USER_PATH + "/contact-requests")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isUnauthorized(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );
        verifyNoInteractions(this.userService);
    }

    // updateContactRequest()
    @Test
    void should204WhenUpdateContactRequestIsSuccessful() throws Exception {
        UpdateContactRequest request = new UpdateContactRequest(FAKER.number().numberBetween(1L, 1000L),
                ContactRequestAction.ACCEPT
        );

        doNothing().when(this.userService).updateContactRequest(eq(request), any(Jwt.class));

        this.mockMvc.perform(put(USER_PATH + "/contact-requests").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpect(status().isNoContent());

        verify(this.userService, times(1)).updateContactRequest(eq(request), any(Jwt.class));
    }

    // updateContactRequest() @Valid
    @Test
    void should400WhenUpdateContactRequestActionIsNull() throws Exception {
        UpdateContactRequest request = new UpdateContactRequest(FAKER.number().numberBetween(1L, 1000L), null);
        String responseBody = """
                {
                    "status": 400,
                    "type": "BAD_REQUEST",
                    "message": "You must provide an action",
                    "path": "/api/v1/user/contact-requests"
                }
                """;

        this.mockMvc.perform(put(USER_PATH + "/contact-requests").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isBadRequest(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.userService);
    }

    // updateContactRequest()
    @Test
    void should401WhenUpdateContactRequestIsCalledByUnauthenticatedUser() throws Exception {
        UpdateContactRequest request = new UpdateContactRequest(FAKER.number().numberBetween(1L, 1000L),
                ContactRequestAction.ACCEPT);

        String responseBody = """
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "/api/v1/user/contact-requests"
                }
                """;

        this.mockMvc.perform(put(USER_PATH + "/contact-requests").with(csrf().asHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isUnauthorized(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.userService);
    }

    // updateContactRequest()
    @Test
    void should403WhenUpdateContactRequestIsCalledWithNoCsrf() throws Exception {
        UpdateContactRequest request = new UpdateContactRequest(FAKER.number().numberBetween(1L, 1000L),
                ContactRequestAction.ACCEPT);

        String responseBody = """
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "/api/v1/user/contact-requests"
                }
                """;

        this.mockMvc.perform(put(USER_PATH + "/contact-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.userService);
    }

    // updateContactRequest()
    @Test
    void should403WhenUpdateContactRequestIsCalledWithInvalidCsrf() throws Exception {
        UpdateContactRequest request = new UpdateContactRequest(FAKER.number().numberBetween(1L, 1000L),
                ContactRequestAction.ACCEPT);

        String responseBody = """
                {
                    "status": 403,
                    "type": "FORBIDDEN",
                    "message": "Access Denied",
                    "path": "/api/v1/user/contact-requests"
                }
                """;

        this.mockMvc.perform(put(USER_PATH + "/contact-requests").with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request))
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isForbidden(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(this.userService);
    }

    // findContacts()
    @Test
    void should200WithListOfContacts() throws Exception {
        UserProfile userProfile1 = new UserProfile(FAKER.number().numberBetween(1L, 1000L), FAKER.internet().username());
        UserProfile userProfile2 = new UserProfile(FAKER.number().numberBetween(1L, 1000L), FAKER.internet().username());
        List<UserProfile> contacts = List.of(userProfile1, userProfile2);

        when(this.userService.findContacts(any(Jwt.class))).thenReturn(contacts);

        this.mockMvc.perform(get(USER_PATH + "/contacts")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(AuthUtils.getAuthentication())))
                .andExpectAll(
                        status().isOk(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(this.objectMapper.writeValueAsString(contacts))
                );
    }

    // findContacts()
    @Test
    void should401WhenFindContactsIsCalledByUnauthorizedUser() throws Exception {
        String responseBody = """
                {
                    "status": 401,
                    "type": "UNAUTHORIZED",
                    "message": "Unauthorized",
                    "path": "/api/v1/user/contacts"
                }
                """;

        this.mockMvc.perform(get(USER_PATH + "/contacts")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpectAll(
                        status().isUnauthorized(),
                        /*
                            By passing false, the false flag it tells the matcher to allow extra fields in the actual
                            response, so it won't fail due to the presence of timestamp.
                         */
                        content().json(responseBody, false)
                );

        verifyNoInteractions(userService);
    }
}
