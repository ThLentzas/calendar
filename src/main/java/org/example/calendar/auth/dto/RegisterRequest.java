package org.example.calendar.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(@NotBlank(message = "The username field is required") String username,
                              @NotBlank(message = "The email field is required") String email,
                              @NotBlank(message = "The password field is required") String password) {
}
