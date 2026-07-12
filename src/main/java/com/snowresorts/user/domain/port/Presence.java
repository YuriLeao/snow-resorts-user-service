package com.snowresorts.user.domain.port;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Tracks recent app activity per user for friend online status. */
public interface Presence {

    void touch(UUID userId);

    Map<UUID, Instant> lastSeenFor(Collection<UUID> userIds);
}
