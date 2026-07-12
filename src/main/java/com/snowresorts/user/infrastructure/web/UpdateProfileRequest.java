package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.domain.model.ShareLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 20, message = "username must be between 3 and 20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username may only contain letters, numbers and underscores")
        String username,

        @NotBlank(message = "displayName is required")
        @Size(max = 100, message = "displayName must be at most 100 characters")
        String displayName,

        @NotNull(message = "shareStats is required")
        ShareLevel shareStats,

        @NotNull(message = "shareLocation is required")
        ShareLevel shareLocation) {
}
