package com.snowresorts.user.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "users", name = "user_presence")
public class UserPresenceEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected UserPresenceEntity() {
    }

    public UserPresenceEntity(UUID userId, Instant lastSeenAt) {
        this.userId = userId;
        this.lastSeenAt = lastSeenAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
