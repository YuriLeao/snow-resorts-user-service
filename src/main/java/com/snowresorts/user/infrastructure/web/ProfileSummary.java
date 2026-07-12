package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.domain.model.Profile;
import java.util.UUID;

/** Public author fields for resort reviews (service-to-service only). */
public record ProfileSummary(UUID userId, String displayName, String avatarUrl) {

    public static ProfileSummary from(Profile profile) {
        return new ProfileSummary(profile.userId(), profile.displayName(), profile.avatarUrl());
    }
}
