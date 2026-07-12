package com.snowresorts.user.infrastructure.persistence;

import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.model.ShareLevel;
import com.snowresorts.user.domain.port.Profiles;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ProfileRepositoryAdapter implements Profiles {

    private final ProfileJpaRepository jpaRepository;

    public ProfileRepositoryAdapter(ProfileJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Profile> findById(UUID userId) {
        return jpaRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public Optional<Profile> findByUsername(String username) {
        return jpaRepository.findByUsernameIgnoreCase(username).map(this::toDomain);
    }

    @Override
    public List<Profile> findAllById(List<UUID> userIds) {
        return jpaRepository.findAllById(userIds).stream().map(this::toDomain).toList();
    }

    @Override
    public Profile save(Profile profile) {
        return toDomain(jpaRepository.save(toEntity(profile)));
    }

    private Profile toDomain(ProfileEntity entity) {
        return new Profile(
                entity.getUserId(),
                entity.getUsername(),
                entity.getDisplayName(),
                entity.getAvatarS3Key(),
                entity.getAvatarUrl(),
                entity.getAvatarUpdatedAt(),
                entity.getLastResortId(),
                ShareLevel.fromDb(entity.getShareStats()),
                ShareLevel.fromDb(entity.getShareLocation()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private ProfileEntity toEntity(Profile profile) {
        return new ProfileEntity(
                profile.userId(),
                profile.username(),
                profile.displayName(),
                profile.avatarS3Key(),
                profile.avatarUrl(),
                profile.avatarUpdatedAt(),
                profile.lastResortId(),
                profile.shareStats().toDb(),
                profile.shareLocation().toDb(),
                profile.createdAt(),
                profile.updatedAt());
    }
}
