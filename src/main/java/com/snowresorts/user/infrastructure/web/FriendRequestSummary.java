package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.domain.model.Friendship;
import com.snowresorts.user.domain.model.Profile;
import java.time.Instant;
import java.util.UUID;

/** Incoming friend request with requester profile fields for the mobile list UI. */
public record FriendRequestSummary(
        UUID userId,
        String username,
        String displayName,
        String avatarUrl,
        Instant requestedAt) {

    public static FriendRequestSummary from(Friendship pending, Profile requester) {
        return new FriendRequestSummary(
                requester.userId(),
                requester.username(),
                requester.displayName(),
                requester.avatarUrl(),
                pending.createdAt());
    }
}
