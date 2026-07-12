package com.snowresorts.user.application;

import com.snowresorts.security.error.BadRequestException;
import com.snowresorts.security.error.ConflictException;
import com.snowresorts.security.error.ResourceNotFoundException;
import com.snowresorts.user.domain.model.Friendship;
import com.snowresorts.user.domain.model.FriendshipStatus;
import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.port.Friendships;
import com.snowresorts.user.domain.port.Profiles;
import com.snowresorts.user.infrastructure.web.FriendRequestSummary;
import com.snowresorts.user.infrastructure.web.FriendSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final PresenceService presenceService;

    public FriendshipService(Friendships friendships, Profiles profiles, PresenceService presenceService) {
        this.friendships = friendships;
        this.profiles = profiles;
        this.presenceService = presenceService;
    }

    @Transactional
    public Friendship requestByUsername(UUID requesterId, String friendUsername) {
        String normalized = ProfileService.requireValidUsername(friendUsername);
        Profile target = profiles.findByUsername(normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with username '%s'.".formatted(normalized)));
        return request(requesterId, target.userId());
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
        return friendships.save(
                new Friendship(requesterId, friendId, FriendshipStatus.PENDING, Instant.now()));
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

    @Transactional
    public void reject(UUID accepterId, UUID requesterId) {
        Friendship pending = friendships.find(requesterId, accepterId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No pending friend request from %s was found.".formatted(requesterId)));
        if (pending.status() != FriendshipStatus.PENDING) {
            throw new ConflictException("Friend request is not pending.");
        }
        friendships.delete(requesterId, accepterId);
        log.info("User {} rejected friendship request from {}", accepterId, requesterId);
    }

    @Transactional
    public void removeFriend(UUID userId, UUID friendId) {
        Friendship edge = friendships.find(userId, friendId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No friendship with %s was found.".formatted(friendId)));
        if (edge.status() != FriendshipStatus.ACCEPTED) {
            throw new ConflictException("Friendship is not active.");
        }
        friendships.delete(userId, friendId);
        friendships.delete(friendId, userId);
        log.info("User {} removed friendship with {}", userId, friendId);
    }

    @Transactional(readOnly = true)
    public List<FriendRequestSummary> listPendingIncomingRequests(UUID userId) {
        List<Friendship> pending = friendships.listPendingIncoming(userId);
        if (pending.isEmpty()) {
            return List.of();
        }
        List<UUID> requesterIds = pending.stream().map(Friendship::userId).toList();
        Map<UUID, Profile> profilesById = profiles.findAllById(requesterIds).stream()
                .collect(Collectors.toMap(Profile::userId, Function.identity()));
        List<FriendRequestSummary> summaries = new ArrayList<>();
        for (Friendship edge : pending) {
            Profile requester = profilesById.get(edge.userId());
            if (requester != null) {
                summaries.add(FriendRequestSummary.from(edge, requester));
            }
        }
        return summaries;
    }

    @Transactional(readOnly = true)
    public List<FriendSummary> listFriendSummaries(UUID userId) {
        List<Profile> friends = listFriends(userId);
        if (friends.isEmpty()) {
            return List.of();
        }
        Map<UUID, Instant> lastSeen = presenceService.lastSeenFor(
                friends.stream().map(Profile::userId).toList());
        return friends.stream()
                .map(friend -> FriendSummary.from(
                        friend,
                        presenceService.isOnline(friend, lastSeen.get(friend.userId()))))
                .toList();
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
