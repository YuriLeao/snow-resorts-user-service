package com.snowresorts.user.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A user's public/private profile. Domain model, never exposed directly on the API.
 * The avatar binary lives in object storage; only the key and a derived public URL are stored.
 */
public record Profile(
        UUID userId,
        String displayName,
        String avatarS3Key,
        String avatarUrl,
        Instant avatarUpdatedAt,
        UUID lastResortId,
        ShareLevel shareStats,
        ShareLevel shareLocation,
        Instant createdAt,
        Instant updatedAt) {

    /** Apply user-editable profile fields, bumping {@code updatedAt}. */
    public Profile withDetails(String newDisplayName, ShareLevel newShareStats,
                               ShareLevel newShareLocation, Instant now) {
        return new Profile(userId, newDisplayName, avatarS3Key, avatarUrl, avatarUpdatedAt,
                lastResortId, newShareStats, newShareLocation, createdAt, now);
    }

    /** Point the avatar at a freshly confirmed object. */
    public Profile withAvatar(String newAvatarS3Key, String newAvatarUrl, Instant now) {
        return new Profile(userId, displayName, newAvatarS3Key, newAvatarUrl, now,
                lastResortId, shareStats, shareLocation, createdAt, now);
    }

    /** Clear all avatar references. */
    public Profile withoutAvatar(Instant now) {
        return new Profile(userId, displayName, null, null, null,
                lastResortId, shareStats, shareLocation, createdAt, now);
    }

    /**
     * A privacy-stripped view of this profile for callers who are not friends:
     * only the display name survives, everything else is hidden.
     */
    public Profile minimalView() {
        return new Profile(userId, displayName, null, null, null,
                null, null, null, null, null);
    }
}
