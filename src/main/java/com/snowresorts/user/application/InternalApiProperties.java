package com.snowresorts.user.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Shared-secret gate for service-to-service internal endpoints.
 *
 * <pre>
 * snow:
 *   internal-api:
 *     secret: ${INTERNAL_API_SECRET:dev-internal-secret}
 *     header: X-Internal-Secret
 * </pre>
 */
@ConfigurationProperties(prefix = "snow.internal-api")
public record InternalApiProperties(String secret, String header) {

    public InternalApiProperties {
        if (secret == null || secret.isBlank()) {
            secret = "dev-internal-secret";
        }
        if (header == null || header.isBlank()) {
            header = "X-Internal-Secret";
        }
    }
}
