package com.snowresorts.user.application;

import com.snowresorts.security.error.BadRequestException;
import com.snowresorts.security.error.ConflictException;
import com.snowresorts.security.error.ResourceNotFoundException;
import com.snowresorts.user.domain.model.FriendshipStatus;
import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.model.ShareLevel;
import com.snowresorts.user.domain.port.Friendships;
import com.snowresorts.user.domain.port.Profiles;
import com.snowresorts.user.infrastructure.web.ProfileSummary;
import java.time.Instant;
import java.util.List;
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
        return bootstrapProfile(userId, null, null, null);
    }

    /**
     * Lazy bootstrap for GET /users/me when no profile row exists yet — auto-generates username
     * and display name from the email (if known) or user id.
     */
    @Transactional
    public Profile bootstrapProfile(UUID userId, String email) {
        return bootstrapProfile(userId, email, null, null);
    }

    /**
     * Creates a profile for a newly registered account. Idempotent — an existing row is
     * returned unchanged. When {@code username} and {@code displayName} are provided (registration
     * flow), they are validated and persisted as chosen by the user.
     */
    @Transactional
    public Profile bootstrapProfile(UUID userId, String email, String username, String displayName) {
        return profiles.findById(userId).orElseGet(() -> {
            Instant now = Instant.now();
            String resolvedUsername = resolveUsernameForBootstrap(username, email, userId);
            String resolvedDisplayName = resolveDisplayName(displayName, email, userId);
            Profile profile = new Profile(userId, resolvedUsername, resolvedDisplayName,
                    null, null, null, null,
                    ShareLevel.FRIENDS, ShareLevel.FRIENDS, now, now);
            log.info("Bootstrapped default profile for user {} (@{})", userId, resolvedUsername);
            return profiles.save(profile);
        });
    }

    /**
     * Validates format and uniqueness of a username before account creation (auth-service).
     *
     * @throws BadRequestException when the username format is invalid
     * @throws ConflictException when the username is already taken
     */
    @Transactional(readOnly = true)
    public void ensureUsernameAvailable(String raw) {
        String normalized = requireValidUsername(raw);
        assertUsernameAvailable(normalized);
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
    public Profile updateMyProfile(UUID userId, String displayName, String username,
                                   ShareLevel shareStats, ShareLevel shareLocation) {
        Profile current = bootstrapProfile(userId, null, null, null);
        String normalizedUsername = requireValidUsername(username);
        if (!normalizedUsername.equals(current.username())) {
            profiles.findByUsername(normalizedUsername).ifPresent(existing -> {
                if (!existing.userId().equals(userId)) {
                    throw new ConflictException("That username is already taken.");
                }
            });
        }
        Profile updated = current.withDetails(normalizedUsername, displayName, shareStats, shareLocation, Instant.now());
        log.info("Updating profile for user {}", userId);
        return profiles.save(updated);
    }

    static String requireValidUsername(String raw) {
        String normalized = UsernameRules.normalize(raw);
        if (!UsernameRules.isValid(normalized)) {
            throw new BadRequestException("username must be 3-20 characters and contain only letters, numbers and underscores.");
        }
        return normalized;
    }

    private String resolveUsernameForBootstrap(String username, String email, UUID userId) {
        if (username != null && !username.isBlank()) {
            String normalized = requireValidUsername(username);
            assertUsernameAvailable(normalized);
            return normalized;
        }
        return allocateUniqueUsername(email, userId);
    }

    private String resolveDisplayName(String displayName, String email, UUID userId) {
        if (displayName != null && !displayName.isBlank()) {
            String trimmed = displayName.trim();
            return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
        }
        return defaultDisplayName(email, userId);
    }

    private void assertUsernameAvailable(String normalized) {
        profiles.findByUsername(normalized).ifPresent(existing -> {
            throw new ConflictException("That username is already taken.");
        });
    }

    private String allocateUniqueUsername(String email, UUID userId) {
        String base = UsernameRules.defaultBase(email, userId);
        if (base.isBlank()) {
            base = UsernameRules.sanitizeBase("user" + userId.toString().substring(0, 8));
        }
        String candidate = base;
        int suffix = 1;
        while (profiles.findByUsername(candidate).isPresent()) {
            candidate = UsernameRules.withNumericSuffix(base, suffix++);
        }
        return candidate;
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

    /**
     * Batch lookup of display names and avatars for public review attribution. Internal callers
     * only — bypasses the friend privacy gate because posting a review is an explicit public act.
     */
    @Transactional(readOnly = true)
    public List<ProfileSummary> listSummaries(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return profiles.findAllById(userIds).stream()
                .map(ProfileSummary::from)
                .toList();
    }
}
