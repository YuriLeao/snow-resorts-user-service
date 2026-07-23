package com.snowresorts.user.application;

import com.snowresorts.security.error.ForbiddenException;
import com.snowresorts.security.error.ResourceNotFoundException;
import com.snowresorts.security.error.UnprocessableEntityException;
import com.snowresorts.security.logging.StructuredLogger;
import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.port.ObjectStorage;
import com.snowresorts.user.domain.port.Profiles;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avatar lifecycle: hand out a presigned PUT URL, confirm the uploaded object (owner + size
 * checks) and delete. The binary never transits this service — only references are stored.
 */
@Service
public class AvatarService {

    private static final Logger log = LoggerFactory.getLogger(AvatarService.class);
    private static final String AVATAR_CONTENT_TYPE = "image/webp";

    private final Profiles profiles;
    private final ObjectStorage objectStorage;
    private final StorageProperties properties;

    public AvatarService(Profiles profiles, ObjectStorage objectStorage, StorageProperties properties) {
        this.profiles = profiles;
        this.objectStorage = objectStorage;
        this.properties = properties;
    }

    /** Presign a PUT for the caller's canonical avatar key {@code avatars/{userId}/current.webp}. */
    public AvatarUploadUrl createUploadUrl(UUID userId) {
        String key = avatarKey(userId);
        String url = objectStorage.presignedPutUrl(key, AVATAR_CONTENT_TYPE, properties.presignTtl());
        StructuredLogger.of(log).info("avatar_presign", "succeeded", "issued",
                "user_id", userId);
        return new AvatarUploadUrl(url, key, properties.presignTtl().toSeconds());
    }

    /**
     * Confirm an uploaded avatar: the key must belong to the caller, the object must exist and be
     * within the size limit. On success the profile's avatar reference is updated.
     */
    @Transactional
    public Profile confirm(UUID userId, String avatarS3Key) {
        String ownerPrefix = ownerPrefix(userId);
        if (avatarS3Key == null || !avatarS3Key.startsWith(ownerPrefix)) {
            throw new ForbiddenException("Avatar key does not belong to the current user.");
        }

        long size = objectStorage.objectSize(avatarS3Key)
                .orElseThrow(() -> new UnprocessableEntityException(
                        "No uploaded object found for the provided avatar key."));
        if (size > properties.maxAvatarBytes()) {
            throw new UnprocessableEntityException(
                    "Avatar exceeds the maximum allowed size of %d bytes.".formatted(properties.maxAvatarBytes()));
        }

        Profile current = profiles.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Profile", userId));

        Instant now = Instant.now();
        String avatarUrl = objectStorage.publicUrl(avatarS3Key) + "?v=" + now.getEpochSecond();
        Profile updated = current.withAvatar(avatarS3Key, avatarUrl, now);
        StructuredLogger.of(log).info("avatar_confirm", "succeeded", "ok",
                "user_id", userId, "size_bytes", size);
        return profiles.save(updated);
    }

    /** Remove the avatar object (if any) and clear the DB references. */
    @Transactional
    public void delete(UUID userId) {
        Profile current = profiles.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Profile", userId));
        if (current.avatarS3Key() != null) {
            objectStorage.delete(current.avatarS3Key());
        }
        profiles.save(current.withoutAvatar(Instant.now()));
        StructuredLogger.of(log).info("avatar_delete", "succeeded", "ok",
                "user_id", userId);
    }

    private static String avatarKey(UUID userId) {
        return ownerPrefix(userId) + "current.webp";
    }

    private static String ownerPrefix(UUID userId) {
        return "avatars/" + userId + "/";
    }
}
