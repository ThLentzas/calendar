package org.example.calendar.auth.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
public class RefreshToken implements Serializable {
    private String tokenValue;
    private Instant issuedAt;
    private Instant expiresAt;
    private Long userId;

    // Will be used by Jackson2Json for deserialization from Redis
    public RefreshToken() {
    }

    public RefreshToken(String tokenValue, Instant issuedAt, Instant expiresAt, Long userId) {
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.tokenValue = tokenValue;
        this.userId = userId;
    }
}
