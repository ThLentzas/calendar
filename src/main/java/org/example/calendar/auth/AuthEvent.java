package org.example.calendar.auth;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.example.calendar.user.UserPrincipal;
import org.example.calendar.utils.CookieUtils;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class AuthEvent {
    private final HttpServletResponse servletResponse;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    /*
        When the user is authenticated via the form login we have to set the refresh and jwt tokens
        https://docs.spring.io/spring-security/reference/servlet/authentication/events.html

        It triggers on every successful authentication, which means for subsequent requests as well when submitting the
        jwt. In that case if we didn't have that if check, the principal would be of type Jwt as the authentication
        is of type JwtAuthenticationToken. We only want to add the auth cookies with the form login.

        UserPrincipal userPrincipal = (UserPrincipal) success.getAuthentication().getPrincipal() => Class cast exception
     */
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent success) {
        if (Objects.requireNonNull(success.getAuthentication().getPrincipal()) instanceof UserPrincipal userPrincipal) {
            String accessTokenValue = this.jwtService.generateToken(userPrincipal);
            String refreshTokenValue = this.refreshTokenService.generateToken(userPrincipal);
            CookieUtils.addAuthCookies(accessTokenValue, 5, refreshTokenValue, 10800, servletResponse);
        }
    }
}