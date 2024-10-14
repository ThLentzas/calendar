package org.example.calendar.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.example.calendar.event.dto.ErrorMessage;
import org.example.calendar.utils.CookieUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;

/*
    If we had the token in the authorization header, as a Bearer token, we would use BearerTokenAuthenticationEntrypoint
    that sets a wwwAuthenticate header according to RFC6750. In our case, what we want is when an access token is invalid,
    malformed or expired, to set a cookie in the response with the same name and maxAge = 0, so the browser removes it.
    Returning 401 would tell the client to go through the refresh process, in either case malformed or expired, and if
    that fails then force them to login.
    https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/bearer-tokens.html#oauth2resourceserver-bearertoken-failure

    If we wanted to add our logic to the BearerTokenAuthenticationEntryPoint class, we could extend that class but, it is
    final. https://devlach.com/blog/java/spring-security-custom-authentication-failure

    I DON'T KNOW IF WE NEED THE BearerTokenAuthenticationEntryPoint in our case since we are not using Bearer tokens.
    If we did the below implementation works. I would just provide my AuthenticationEntrypoint implementation and for
    the same logic my AccessDeniedHandler.

    public class CookieTokenAuthenticationEntrypoint implements AuthenticationEntryPoint {
        private final AuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();

        @Override
        public void commence(HttpServletRequest request,
                             HttpServletResponse response,
                             AuthenticationException authException) throws ServletException, IOException {
            this.delegate.commence(request, response, authException);
            Cookie cookie = CookieUtils.generateCookie("accessToken", "", "/", 0);
            response.addCookie(cookie);
        }
    }

    Going for the approach below makes testing really hard, because we would have to mock the beans when the SecurityConfig
    class is imported in our @WebMvcTest and, we will not be able to check for 401 and 403 cases because both the
    EntryPoint and AccessDeniedHandler are mocked.

    @Component
    @RequiredArgsConstructor
    public final class CookieTokenAuthenticationEntrypoint implements AuthenticationEntryPoint {
        private final ObjectMapper objectMapper;

        @Override
        public void commence(HttpServletRequest request,
                             HttpServletResponse response,
                             AuthenticationException authException) throws IOException {
            ErrorMessage errorMessage = new ErrorMessage(Instant.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    ErrorMessage.ErrorType.UNAUTHORIZED,
                    "Unauthorized",
                    request.getRequestURI());
            Cookie cookie = CookieUtils.generateCookie("accessToken", "", "/", 0);
            response.addCookie(cookie);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write(this.objectMapper.writeValueAsString(errorMessage));
        }
        When an unauthenticated user makes the request that results in 401, that will tell the client to make /refresh
        request but since the refresh token will be empty the /refresh will result in 401 telling the client to log in
        the user
 */
public final class CookieTokenAuthenticationEntrypoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(), HttpStatus.UNAUTHORIZED.value(), ErrorMessage.ErrorType.UNAUTHORIZED, "Unauthorized", request.getRequestURI());
        Cookie cookie = CookieUtils.generateCookie("accessToken", "", "/", 0);
        response.addCookie(cookie);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.getWriter().write(objectMapper.writeValueAsString(errorMessage));
    }
}
