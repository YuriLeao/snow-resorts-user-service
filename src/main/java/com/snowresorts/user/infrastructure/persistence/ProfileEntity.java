package com.snowresorts.user.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "users", name = "profiles")
public class ProfileEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String username;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "avatar_s3_key")
    private String avatarS3Key;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "avatar_updated_at")
    private Instant avatarUpdatedAt;

    @Column(name = "last_resort_id")
    private UUID lastResortId;

    @Column(name = "share_stats", nullable = false)
    private String shareStats;

    @Column(name = "share_location", nullable = false)
    private String shareLocation;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProfileEntity() {
    }

    public ProfileEntity(UUID userId, String username, String displayName, String avatarS3Key, String avatarUrl,
                         Instant avatarUpdatedAt, UUID lastResortId, String shareStats,
                         String shareLocation, Instant createdAt, Instant updatedAt) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.avatarS3Key = avatarS3Key;
        this.avatarUrl = avatarUrl;
        this.avatarUpdatedAt = avatarUpdatedAt;
        this.lastResortId = lastResortId;
        this.shareStats = shareStats;
        this.shareLocation = shareLocation;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarS3Key() {
        return avatarS3Key;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Instant getAvatarUpdatedAt() {
        return avatarUpdatedAt;
    }

    public UUID getLastResortId() {
        return lastResortId;
    }

    public String getShareStats() {
        return shareStats;
    }

    public String getShareLocation() {
        return shareLocation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
