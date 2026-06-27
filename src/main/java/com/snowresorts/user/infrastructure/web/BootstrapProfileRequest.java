package com.snowresorts.user.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Payload for the internal profile bootstrap endpoint (auth-service → user-service). */
public record BootstrapProfileRequest(
        @NotNull(message = "userId is required")
        UUID userId,
        @NotBlank(message = "email is required")
        @Email(message = "must be a well-formed email address")
        String email) {
}
