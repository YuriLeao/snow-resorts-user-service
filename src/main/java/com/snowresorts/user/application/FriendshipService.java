package com.snowresorts.user.application;

import com.snowresorts.security.error.BadRequestException;
import com.snowresorts.security.error.ConflictException;
import com.snowresorts.security.error.ResourceNotFoundException;
import com.snowresorts.user.domain.model.Friendship;
import com.snowresorts.user.domain.model.FriendshipStatus;
import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.port.Friendships;
import com.snowresorts.user.domain.port.Profiles;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Friendship use cases. A request creates a single PENDING edge (requester -> target). Accepting
 * stores ACCEPTED rows in <em>both</em> directions so that listing accepted friends and the
 * profile-visibility check are simple symmetric lookups.
 */
@Service
public class FriendshipService {

    private static final Logger log = LoggerFactory.getLogger(FriendshipService.class);

    private final Friendships friendships;
    private final Profiles profiles;

    public FriendshipService(Friendships friendships, Profiles profiles) {
        this.friendships = friendships;
        this.profiles = profiles;
    }

    @Transactional
    public Friendship request(UUID requesterId, UUID friendId) {
        if (requesterId.equals(friendId)) {
            throw new BadRequestException("You cannot send a friend request to yourself.");
        }
        if (friendships.find(requesterId, friendId).isPresent()) {
            throw new ConflictException("A friendship or pending request already exists.");
        }
        log.info("User {} requested friendship with {}", requesterId, friendId);
        return friendships.save(new Friendship(requesterId, friendId, FriendshipStatus.PENDING, Instant.now()));
    }

    /**
     * Accept the PENDING request from {@code requesterId} to the calling user. Creates/updates
     * ACCEPTED edges in both directions.
     */
    @Transactional
    public void accept(UUID accepterId, UUID requesterId) {
        Friendship pending = friendships.find(requesterId, accepterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pending friend request from %s was found.".formatted(requesterId)));
        if (pending.status() != FriendshipStatus.PENDING) {
            throw new ConflictException("Friend request is not pending.");
        }

        Instant now = Instant.now();
        friendships.save(new Friendship(requesterId, accepterId, FriendshipStatus.ACCEPTED, pending.createdAt()));
        friendships.save(new Friendship(accepterId, requesterId, FriendshipStatus.ACCEPTED, now));
        log.info("User {} accepted friendship request from {}", accepterId, requesterId);
    }

    @Transactional(readOnly = true)
    public List<Profile> listFriends(UUID userId) {
        List<UUID> friendIds = friendships.listAccepted(userId).stream()
                .map(Friendship::friendId)
                .toList();
        if (friendIds.isEmpty()) {
            return List.of();
        }
        return profiles.findAllById(friendIds);
    }
}
