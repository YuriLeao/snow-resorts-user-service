package com.snowresorts.user.application;

import com.snowresorts.security.error.ResourceNotFoundException;
import com.snowresorts.user.domain.model.FriendshipStatus;
import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.model.ShareLevel;
import com.snowresorts.user.domain.port.Friendships;
import com.snowresorts.user.domain.port.Profiles;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profile read/update use cases plus the friend-visibility (privacy) gate that decides whether
 * a caller may see another user's avatar and stats.
 */
@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final Profiles profiles;
    private final Friendships friendships;

    public ProfileService(Profiles profiles, Friendships friendships) {
        this.profiles = profiles;
        this.friendships = friendships;
    }

    @Transactional
    public Profile getMyProfile(UUID userId) {
        // Accounts can exist in auth-service before user-service receives the internal bootstrap
        // call (e.g. user-service was down at registration). Auto-create on first read so
        // GET /users/me never returns 404 for a valid JWT.
        return bootstrapProfile(userId, null);
    }

    /**
     * Creates a default profile for a newly registered account. Idempotent — an existing row is
     * returned unchanged.
     */
    @Transactional
    public Profile bootstrapProfile(UUID userId, String email) {
        return profiles.findById(userId).orElseGet(() -> {
            Instant now = Instant.now();
            Profile profile = new Profile(userId, defaultDisplayName(email, userId),
                    null, null, null, null,
                    ShareLevel.FRIENDS, ShareLevel.FRIENDS, now, now);
            log.info("Bootstrapped default profile for user {}", userId);
            return profiles.save(profile);
        });
    }

    static String defaultDisplayName(String email, UUID userId) {
        if (email != null && email.contains("@")) {
            String localPart = email.substring(0, email.indexOf('@'));
            String sanitized = localPart.replaceAll("[^a-zA-Z0-9._-]", "");
            if (!sanitized.isBlank()) {
                return sanitized.length() > 100 ? sanitized.substring(0, 100) : sanitized;
            }
        }
        return "User" + userId.toString().substring(0, 8);
    }

    @Transactional
    public Profile updateMyProfile(UUID userId, String displayName,
                                   ShareLevel shareStats, ShareLevel shareLocation) {
        Profile current = bootstrapProfile(userId, null);
        Profile updated = current.withDetails(displayName, shareStats, shareLocation, Instant.now());
        log.info("Updating profile for user {}", userId);
        return profiles.save(updated);
    }

    /**
     * Fetch another user's profile applying the privacy gate: full profile (avatar + stats) only
     * when the caller is the owner or an accepted friend; otherwise a minimal display-name-only view.
     */
    @Transactional(readOnly = true)
    public Profile getUser(UUID callerId, UUID targetId) {
        Profile target = profiles.findById(targetId)
                .orElseThrow(() -> ResourceNotFoundException.of("Profile", targetId));

        if (callerId.equals(targetId) || areFriends(callerId, targetId)) {
            return target;
        }
        return target.minimalView();
    }

    private boolean areFriends(UUID callerId, UUID targetId) {
        return friendships.find(callerId, targetId)
                .map(f -> f.status() == FriendshipStatus.ACCEPTED)
                .orElse(false);
    }
}
