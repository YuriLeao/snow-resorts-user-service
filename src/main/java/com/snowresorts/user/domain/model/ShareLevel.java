package com.snowresorts.user.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.snowresorts.security.error.BadRequestException;

/**
 * Visibility level for privacy-gated profile data (stats, live location).
 * Persisted and exposed over the API in lower-case ({@code friends}/{@code nobody}) to match
 * the schema defaults; request parsing is case-insensitive.
 */
public enum ShareLevel {
    FRIENDS,
    NOBODY;

    @JsonValue
    public String toDb() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static ShareLevel fromDb(String value) {
        if (value == null || value.isBlank()) {
            return FRIENDS;
        }
        return switch (value.trim().toLowerCase()) {
            case "friends" -> FRIENDS;
            case "nobody" -> NOBODY;
            default -> throw new BadRequestException("Unknown share level: " + value);
        };
    }
}
