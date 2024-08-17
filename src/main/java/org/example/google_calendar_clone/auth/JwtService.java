package org.example.google_calendar_clone.auth;

import org.example.google_calendar_clone.user.UserPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class JwtService {
    private final JwtEncoder jwtEncoder;

    /*
        I decided to not write any tests for this method because apart from passing the arguments to the function there
        is not much to assert. We can mock the password and assert on the token value but, we don't test much, we don't
        have access to the entire jwt, we could assert on the claims but, now we only have the token value.
     */
    String generateToken(UserPrincipal userPrincipal) {
        Instant now = Instant.now();
        //5 minutes TTL
        long expiry = 300L;

        String authorities = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .subject(userPrincipal.user().getId().toString())
                .claim("authorities", authorities)
                .build();

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
