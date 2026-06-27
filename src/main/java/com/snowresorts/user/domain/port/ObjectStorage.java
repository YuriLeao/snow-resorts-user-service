package com.snowresorts.user.domain.port;

import java.time.Duration;
import java.util.Optional;

/**
 * Outbound port for the avatar object store. Implemented by an S3/MinIO adapter so the
 * mobile client can upload binaries directly via presigned URLs (no binary through the service).
 */
public interface ObjectStorage {

    /** A presigned HTTP PUT URL the client uses to upload the object directly. */
    String presignedPutUrl(String key, String contentType, Duration ttl);

    /** Size of the stored object in bytes, or empty when the object does not exist. */
    Optional<Long> objectSize(String key);

    void delete(String key);

    /** Stable, publicly reachable URL (CDN/MinIO) for an object key. */
    String publicUrl(String key);
}
