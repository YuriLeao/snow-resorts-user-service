package com.snowresorts.user.application;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Validation and normalization for public @usernames. */
public final class UsernameRules {

    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,20}$");

    private UsernameRules() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("@")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String normalized) {
        return normalized != null && USERNAME_PATTERN.matcher(normalized).matches();
    }

    /** Base username before uniqueness suffix — from email local-part or user id prefix. */
    public static String defaultBase(String email, UUID userId) {
        if (email != null && email.contains("@")) {
            String localPart = email.substring(0, email.indexOf('@'));
            String sanitized = sanitizeBase(localPart);
            if (!sanitized.isBlank()) {
                return sanitized;
            }
        }
        return sanitizeBase("user" + userId.toString().substring(0, 8));
    }

    public static String sanitizeBase(String raw) {
        if (raw == null) {
            return "";
        }
        String sanitized = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        if (sanitized.length() < 3) {
            return "";
        }
        return sanitized.length() > 20 ? sanitized.substring(0, 20) : sanitized;
    }

    public static String withNumericSuffix(String base, int suffix) {
        String suffixText = String.valueOf(suffix);
        int maxBase = 20 - suffixText.length();
        if (maxBase < 1) {
            return suffixText.substring(0, Math.min(20, suffixText.length()));
        }
        String trimmedBase = base.length() > maxBase ? base.substring(0, maxBase) : base;
        return trimmedBase + suffixText;
    }
}
