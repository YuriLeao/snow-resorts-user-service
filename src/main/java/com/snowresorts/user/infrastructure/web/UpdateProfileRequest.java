package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.domain.model.ShareLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "displayName is required")
        @Size(max = 100, message = "displayName must be at most 100 characters")
        String displayName,

        @NotNull(message = "shareStats is required")
        ShareLevel shareStats,

        @NotNull(message = "shareLocation is required")
        ShareLevel shareLocation) {
}
