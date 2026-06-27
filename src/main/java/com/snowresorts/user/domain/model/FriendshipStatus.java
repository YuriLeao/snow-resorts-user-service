package com.snowresorts.user.domain.model;

/** Lifecycle state of a friendship edge. Persisted as the upper-case enum name. */
public enum FriendshipStatus {
    PENDING,
    ACCEPTED,
    BLOCKED
}
