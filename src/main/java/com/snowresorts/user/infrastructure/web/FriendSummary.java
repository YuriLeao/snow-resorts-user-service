package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.domain.model.Profile;
import java.time.Instant;
import java.util.UUID;

public record FriendSummary(
        UUID userId,
        String displayName,
        String avatarUrl,
        String status,
        boolean online,
        UUID currentResortId) {

    public static FriendSummary from(Profile profile, boolean online) {
        return new FriendSummary(
                profile.userId(),
                profile.displayName(),
                profile.avatarUrl(),
                "accepted",
                online,
                online ? profile.lastResortId() : null);
    }
}
