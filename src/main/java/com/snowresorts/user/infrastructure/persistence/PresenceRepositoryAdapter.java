package com.snowresorts.user.infrastructure.persistence;

import com.snowresorts.user.domain.port.Presence;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class PresenceRepositoryAdapter implements Presence {

    private final UserPresenceJpaRepository jpaRepository;

    public PresenceRepositoryAdapter(UserPresenceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void touch(UUID userId) {
        Instant now = Instant.now();
        jpaRepository.findById(userId)
                .ifPresentOrElse(
                        row -> {
                            row.setLastSeenAt(now);
                            jpaRepository.save(row);
                        },
                        () -> jpaRepository.save(new UserPresenceEntity(userId, now)));
    }

    @Override
    public Map<UUID, Instant> lastSeenFor(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return jpaRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserPresenceEntity::getUserId, UserPresenceEntity::getLastSeenAt));
    }
}
