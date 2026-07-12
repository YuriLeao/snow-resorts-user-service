package com.snowresorts.user.infrastructure.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ProfileSummariesRequest(
        @NotEmpty(message = "userIds must not be empty")
        @Size(max = 100, message = "userIds must contain at most 100 entries")
        List<UUID> userIds) {
}
