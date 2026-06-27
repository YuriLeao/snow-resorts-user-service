package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.domain.model.Profile;
import java.util.UUID;

public record FriendSummary(UUID userId, String displayName, String avatarUrl) {

    public static FriendSummary from(Profile profile) {
        return new FriendSummary(profile.userId(), profile.displayName(), profile.avatarUrl());
    }
}
