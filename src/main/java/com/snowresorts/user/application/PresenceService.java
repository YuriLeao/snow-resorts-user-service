package com.snowresorts.user.application;

import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.model.ShareLevel;
import com.snowresorts.user.domain.port.Presence;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresenceService {

    static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);

    private final Presence presence;

    public PresenceService(Presence presence) {
        this.presence = presence;
    }

    @Transactional
    public void touch(UUID userId) {
        presence.touch(userId);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Instant> lastSeenFor(Collection<UUID> userIds) {
        return presence.lastSeenFor(userIds);
    }

    public boolean isOnline(Profile friend, Instant lastSeenAt) {
        if (friend.shareLocation() == ShareLevel.NOBODY) {
            return false;
        }
        if (lastSeenAt == null) {
            return false;
        }
        return lastSeenAt.isAfter(Instant.now().minus(ONLINE_WINDOW));
    }
}
