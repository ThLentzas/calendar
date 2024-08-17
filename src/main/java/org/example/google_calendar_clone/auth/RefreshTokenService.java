package org.example.google_calendar_clone.auth;

import org.example.google_calendar_clone.auth.dto.RefreshToken;
import org.example.google_calendar_clone.exception.UnauthorizedException;
import org.example.google_calendar_clone.redis.RedisClient;
import org.example.google_calendar_clone.user.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

// Refresh tokens are single use
@Service
@RequiredArgsConstructor
class RefreshTokenService {
    private final RedisClient redisClient;
    private final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    String generateToken(UserPrincipal userPrincipal) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken refreshToken = new RefreshToken(generateKey(), issuedAt, expiresAt, userPrincipal.user().getId());
        this.redisClient.set(refreshToken.getTokenValue(), refreshToken, 7, TimeUnit.DAYS);

        return refreshToken.getTokenValue();
    }

    RefreshToken findByTokenValue(String tokenValue) {
        RefreshToken refreshToken = this.redisClient.get(tokenValue);
        // When the refresh token expires, the user must log in again
        if(refreshToken == null) {
            logger.info("Refresh token expired with value: {}", tokenValue);
            throw new UnauthorizedException("Unauthorized");
        }
        // We don't have to check if the token expired because the TTL is the same as its maxAge, 7 days. It can be in Redis and expired
        return  refreshToken;
    }

    void deleteToken(String tokenValue) {
        this.redisClient.delete(tokenValue);
    }

    private String generateKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[96];
        secureRandom.nextBytes(randomBytes);
        byte[] base64EncodedKey = Base64.getUrlEncoder()
                .withoutPadding()
                .encode(randomBytes);

        return new String(base64EncodedKey);
    }
}
