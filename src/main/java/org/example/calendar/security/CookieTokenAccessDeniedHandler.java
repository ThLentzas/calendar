package org.example.calendar.security;

import org.example.calendar.event.dto.ErrorMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;

/*
    If we had the token in the authorization header, as a Bearer token, we would use BearerTokenAccessDeniedHandler
    that sets a wwwAuthenticate header according to RFC6750. In our case, what we want is to write in the response body.
    https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/bearer-tokens.html#oauth2resourceserver-bearertoken-failure

    If we wanted to add our logic to the BearerTokenAccessDeniedHandler class, we could extend that class but, it is
    final. https://devlach.com/blog/java/spring-security-custom-authentication-failure

    I DON'T KNOW IF WE NEED THE BearerTokenAccessDeniedHandler in our case since we are not using Bearer tokens.
    If we did the below implementation works. I would just provide my AuthenticationEntrypoint implementation and for
    the same logic my AccessDeniedHandler.

    Look at the AuthenticationEntryPoint for why we add the ObjectMapper.
 */
public final class CookieTokenAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        ErrorMessage errorMessage = new ErrorMessage(Instant.now(), HttpStatus.FORBIDDEN.value(), ErrorMessage.ErrorType.FORBIDDEN, "Access Denied", request.getRequestURI());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.getWriter().write(objectMapper.writeValueAsString(errorMessage));
    }
}
