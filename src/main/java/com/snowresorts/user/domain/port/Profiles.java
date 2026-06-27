package com.snowresorts.user.domain.port;

import com.snowresorts.user.domain.model.Profile;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

/** Outbound port for reading and persisting user profiles. */
public interface Profiles {

    Optional<Profile> findById(UUID userId);

    List<Profile> findAllById(List<UUID> userIds);

    Profile save(Profile profile);
}
