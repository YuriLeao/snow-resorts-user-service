package com.snowresorts.user.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record FriendRequestRequest(
        @NotNull(message = "friendId is required")
        UUID friendId) {
}
