package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.model.ShareLevel;
import java.time.Instant;
import java.util.UUID;

/**
 * API representation of a profile. For privacy-gated views of other users, the avatar and
 * stats-related fields are {@code null} (the domain returns a minimal view).
 */
public record ProfileResponse(
        UUID userId,
        String username,
        String displayName,
        String avatarUrl,
        Instant avatarUpdatedAt,
        UUID lastResortId,
        ShareLevel shareStats,
        ShareLevel shareLocation) {

    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
                profile.userId(),
                profile.username(),
                profile.displayName(),
                profile.avatarUrl(),
                profile.avatarUpdatedAt(),
                profile.lastResortId(),
                profile.shareStats(),
                profile.shareLocation());
    }
}
