package org.example.calendar.event.dto;

import java.time.Instant;

public record ErrorMessage(Instant timestamp,
                    Integer status,
                    ErrorType type,
                    String message,
                    String path) {
    public enum ErrorType {
        BAD_REQUEST,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT,
        INTERNAL_SERVER_ERROR
    }
}
