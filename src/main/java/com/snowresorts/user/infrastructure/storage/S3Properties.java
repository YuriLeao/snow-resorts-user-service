package com.snowresorts.user.infrastructure.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3/MinIO client configuration.
 *
 * <pre>
 * snow:
 *   storage:
 *     s3:
 *       endpoint: http://localhost:9000   # blank on real AWS
 *       region: us-east-1
 *       access-key: minioadmin            # blank -> default credentials provider
 *       secret-key: minioadmin
 *       path-style-access: true           # true for MinIO, false for real S3
 * </pre>
 */
@ConfigurationProperties(prefix = "snow.storage.s3")
public record S3Properties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        boolean pathStyleAccess) {

    public S3Properties {
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }
    }

    public boolean hasEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }

    public boolean hasStaticCredentials() {
        return accessKey != null && !accessKey.isBlank();
    }
}
