package com.snowresorts.user.infrastructure.persistence;

import com.snowresorts.user.domain.model.Friendship;
import com.snowresorts.user.domain.model.FriendshipStatus;
import com.snowresorts.user.domain.port.Friendships;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class FriendshipRepositoryAdapter implements Friendships {

    private final FriendshipJpaRepository jpaRepository;

    public FriendshipRepositoryAdapter(FriendshipJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Friendship> find(UUID userId, UUID friendId) {
        return jpaRepository.findByUserIdAndFriendId(userId, friendId).map(this::toDomain);
    }

    @Override
    public Friendship save(Friendship friendship) {
        return toDomain(jpaRepository.save(toEntity(friendship)));
    }

    @Override
    public List<Friendship> listAccepted(UUID userId) {
        return jpaRepository.findByUserIdAndStatus(userId, FriendshipStatus.ACCEPTED.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Friendship> listPendingIncoming(UUID userId) {
        return jpaRepository.findByFriendIdAndStatus(userId, FriendshipStatus.PENDING.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID userId, UUID friendId) {
        jpaRepository.findByUserIdAndFriendId(userId, friendId)
                .ifPresent(jpaRepository::delete);
    }

    private Friendship toDomain(FriendshipEntity entity) {
        return new Friendship(
                entity.getUserId(),
                entity.getFriendId(),
                FriendshipStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt());
    }

    private FriendshipEntity toEntity(Friendship friendship) {
        return new FriendshipEntity(
                friendship.userId(),
                friendship.friendId(),
                friendship.status().name(),
                friendship.createdAt());
    }
}
