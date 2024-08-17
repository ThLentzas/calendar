package org.example.google_calendar_clone.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.example.google_calendar_clone.utils.CookieUtils;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import jakarta.servlet.http.Cookie;

@Configuration
public class JwtConfig {
    @Value("${rsa.private_key}")
    private RSAPrivateKey privateKey;
    @Value("${rsa.public_key}")
    private RSAPublicKey publicKey;

    @Bean
    JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(this.publicKey).privateKey(this.privateKey).build();
        // Not the SecurityContext from SpringSecurity but from Nimbus
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));

        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.publicKey).build();
    }

    /**
     * Configures a JWT authentication converter to extract authorities from the JWT token.
     * https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html#oauth2resourceserver-jwt-authorization-extraction
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName("authorities");
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

    /*
        The filter that gets invoked is the BearerTokenAuthenticationFilter. Handles the token and set the Authentication
        of type JwtAuthenticationToken

        https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html#oauth2resourceserver-jwt-authorization-extraction

        Currently, Spring does not provide a way to read the Jwt from an HttpOnly Cookie, so we provide our logic for extraction
        by publishing a BearerTokenResolver. It is added for Spring 6.4.x
        https://github.com/spring-projects/spring-security/issues/9230

        It is a functional interface. An alternative would be an Anonymous class
        BearerTokenResolver customBearerTokenResolver() {
            return new BearerTokenResolver() {
                @Override
                public String resolve(HttpServletRequest request)
            }

        If we return null, it means the request was made from a non-authenticated user and there is no cookie in the
        request
     */
    @Bean
    BearerTokenResolver cookieTokenResolver() {
        return request -> {
            Cookie cookie = CookieUtils.getCookie(request, "ACCESS_TOKEN");
            return cookie == null ? null : cookie.getValue();
        };
    }
}
