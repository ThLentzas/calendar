package org.example.calendar;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Collections;

public final class AuthUtils {

    private AuthUtils() {
        // prevent instantiation
        throw new UnsupportedOperationException("AuthUtils is a utility class and cannot be instantiated");
    }

    // If we need more or different roles we can just pass an array/list in the function and convert them
    // https://docs.spring.io/spring-security/reference/servlet/test/mockmvc/oauth2.html#_jwt_requestpostprocessor
    public static Authentication getAuthentication() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject("1")  // userId as subject
                .build();

        Collection<GrantedAuthority> authorities = Collections.emptyList();
        return new JwtAuthenticationToken(jwt, authorities);
    }
}
