package com.snowresorts.user.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A directed friendship edge ({@code userId} -> {@code friendId}). Accepted friendships are
 * stored symmetrically (one row per direction) so listing a user's friends is a simple lookup.
 */
public record Friendship(
        UUID userId,
        UUID friendId,
        FriendshipStatus status,
        Instant createdAt) {
}
