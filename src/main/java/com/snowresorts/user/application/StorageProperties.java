package com.snowresorts.user.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Avatar storage configuration.
 *
 * <pre>
 * snow:
 *   storage:
 *     bucket: snow-resorts-assets
 *     public-base-url: http://localhost:9000/snow-resorts-assets
 *     presign-ttl: 10m
 *     max-avatar-bytes: 2097152
 * </pre>
 */
@ConfigurationProperties(prefix = "snow.storage")
public record StorageProperties(
        String bucket,
        String publicBaseUrl,
        Duration presignTtl,
        long maxAvatarBytes) {

    public StorageProperties {
        if (bucket == null || bucket.isBlank()) {
            bucket = "snow-resorts-assets";
        }
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            publicBaseUrl = "http://localhost:9000/snow-resorts-assets";
        }
        if (presignTtl == null) {
            presignTtl = Duration.ofMinutes(10);
        }
        if (maxAvatarBytes <= 0) {
            maxAvatarBytes = 2_097_152;
        }
    }
}
