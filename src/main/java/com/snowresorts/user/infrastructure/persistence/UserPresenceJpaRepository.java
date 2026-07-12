package com.snowresorts.user.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPresenceJpaRepository extends JpaRepository<UserPresenceEntity, UUID> {

    List<UserPresenceEntity> findByUserIdIn(Collection<UUID> userIds);
}
