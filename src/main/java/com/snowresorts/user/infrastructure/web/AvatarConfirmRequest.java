package com.snowresorts.user.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AvatarConfirmRequest(
        @NotBlank(message = "avatarS3Key is required")
        @Size(max = 512, message = "avatarS3Key is too long")
        String avatarS3Key) {
}
