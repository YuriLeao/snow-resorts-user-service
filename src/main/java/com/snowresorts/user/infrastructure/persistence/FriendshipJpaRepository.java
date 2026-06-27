package com.snowresorts.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendshipJpaRepository extends JpaRepository<FriendshipEntity, FriendshipId> {

    Optional<FriendshipEntity> findByUserIdAndFriendId(UUID userId, UUID friendId);

    List<FriendshipEntity> findByUserIdAndStatus(UUID userId, String status);
}
