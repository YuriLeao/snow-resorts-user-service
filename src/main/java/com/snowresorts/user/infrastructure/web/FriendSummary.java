package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.model.ShareLevel;
import java.util.UUID;

public record FriendSummary(
        UUID userId,
        String displayName,
        String avatarUrl,
        String status,
        boolean online,
        UUID currentResortId,
        /** Whether this friend shares descent stats with friends (for ranking / history). */
        ShareLevel shareStats) {

    public static FriendSummary from(Profile profile, boolean online) {
        return new FriendSummary(
                profile.userId(),
                profile.displayName(),
                profile.avatarUrl(),
                "accepted",
                online,
                online ? profile.lastResortId() : null,
                profile.shareStats());
    }
}
