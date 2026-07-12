package com.snowresorts.user.domain.port;

import com.snowresorts.user.domain.model.Profile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for reading and persisting user profiles. */
public interface Profiles {

    Optional<Profile> findById(UUID userId);

    Optional<Profile> findByUsername(String username);

    List<Profile> findAllById(List<UUID> userIds);

    Profile save(Profile profile);
}
