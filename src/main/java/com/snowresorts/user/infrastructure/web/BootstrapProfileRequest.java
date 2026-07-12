package com.snowresorts.user.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Payload for the internal profile bootstrap endpoint (auth-service → user-service). */
public record BootstrapProfileRequest(
        @NotNull(message = "userId is required")
        UUID userId,
        @NotBlank(message = "email is required")
        @Email(message = "must be a well-formed email address")
        String email,
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 20, message = "username must be between 3 and 20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username may only contain letters, numbers and underscores")
        String username,
        @NotBlank(message = "displayName is required")
        @Size(max = 100, message = "displayName must be at most 100 characters")
        String displayName) {
}
