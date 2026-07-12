package com.snowresorts.user.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FriendRequestRequest(
        @NotBlank(message = "friendUsername is required")
        @Size(min = 3, max = 20, message = "friendUsername must be between 3 and 20 characters")
        @Pattern(regexp = "^@?[a-zA-Z0-9_]+$", message = "friendUsername may only contain letters, numbers and underscores")
        String friendUsername) {
}
