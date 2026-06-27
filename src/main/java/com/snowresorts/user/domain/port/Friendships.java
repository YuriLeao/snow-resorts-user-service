package com.snowresorts.user.domain.port;

import com.snowresorts.user.domain.model.Friendship;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for reading and persisting friendship edges. */
public interface Friendships {

    Optional<Friendship> find(UUID userId, UUID friendId);

    Friendship save(Friendship friendship);

    /** Accepted friendships where {@code userId} is the owner of the edge. */
    List<Friendship> listAccepted(UUID userId);
}
