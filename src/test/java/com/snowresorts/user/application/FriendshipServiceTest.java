package com.snowresorts.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snowresorts.security.error.ConflictException;
import com.snowresorts.security.error.ResourceNotFoundException;
import com.snowresorts.user.domain.model.Friendship;
import com.snowresorts.user.domain.model.FriendshipStatus;
import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.model.ShareLevel;
import com.snowresorts.user.domain.port.Friendships;
import com.snowresorts.user.domain.port.Profiles;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceTest {

    private static final UUID REQUESTER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TARGET = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private Friendships friendships;
    @Mock
    private Profiles profiles;
    @Mock
    private PresenceService presenceService;

    private FriendshipService service;

    @BeforeEach
    void setUp() {
        service = new FriendshipService(friendships, profiles, presenceService);
    }

    private Profile requesterProfile() {
        return new Profile(REQUESTER, "requester", "Requester", null, null, null, null,
                ShareLevel.FRIENDS, ShareLevel.FRIENDS, Instant.now(), Instant.now());
    }

    private Profile targetProfile() {
        return new Profile(TARGET, "target_user", "Target", null, null, null, null,
                ShareLevel.FRIENDS, ShareLevel.FRIENDS, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("requestByUsername resolves the profile and creates a PENDING edge")
    void requestByUsername_whenUserExists_createsPending() {
        when(profiles.findByUsername("target_user")).thenReturn(Optional.of(targetProfile()));
        when(friendships.find(REQUESTER, TARGET)).thenReturn(Optional.empty());
        when(friendships.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profiles.findById(REQUESTER)).thenReturn(Optional.of(requesterProfile()));

        Friendship created = service.requestByUsername(REQUESTER, "@target_user");

        assertThat(created.status()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(created.friendId()).isEqualTo(TARGET);
    }

    @Test
    @DisplayName("requestByUsername when username is unknown returns 404")
    void requestByUsername_whenUnknown_throwsNotFound() {
        when(profiles.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestByUsername(REQUESTER, "missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("request creates a PENDING edge from requester to target")
    void request_whenNoneExists_createsPending() {
        when(friendships.find(REQUESTER, TARGET)).thenReturn(Optional.empty());
        when(friendships.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profiles.findById(REQUESTER)).thenReturn(Optional.of(requesterProfile()));

        Friendship created = service.request(REQUESTER, TARGET);

        assertThat(created.status()).isEqualTo(FriendshipStatus.PENDING);
        assertThat(created.userId()).isEqualTo(REQUESTER);
        assertThat(created.friendId()).isEqualTo(TARGET);
    }

    @Test
    @DisplayName("request when a friendship/request already exists returns 409 Conflict")
    void request_whenAlreadyExists_throwsConflict() {
        when(friendships.find(REQUESTER, TARGET)).thenReturn(Optional.of(
                new Friendship(REQUESTER, TARGET, FriendshipStatus.PENDING, Instant.now())));

        assertThatThrownBy(() -> service.request(REQUESTER, TARGET))
                .isInstanceOf(ConflictException.class);

        verify(friendships, never()).save(any());
    }

    @Test
    @DisplayName("accept transitions the pending request to ACCEPTED in both directions")
    void accept_withPendingRequest_transitionsToAccepted() {
        when(friendships.find(REQUESTER, TARGET)).thenReturn(Optional.of(
                new Friendship(REQUESTER, TARGET, FriendshipStatus.PENDING, Instant.now())));
        when(friendships.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));

        service.accept(TARGET, REQUESTER);

        ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
        verify(friendships, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(f -> f.status() == FriendshipStatus.ACCEPTED);
        assertThat(captor.getAllValues())
                .anyMatch(f -> f.userId().equals(REQUESTER) && f.friendId().equals(TARGET))
                .anyMatch(f -> f.userId().equals(TARGET) && f.friendId().equals(REQUESTER));
    }

    @Test
    @DisplayName("listPendingIncomingRequests returns requester profiles for incoming PENDING edges")
    void listPendingIncomingRequests_withPending_returnsSummaries() {
        Instant requestedAt = Instant.parse("2026-01-01T12:00:00Z");
        when(friendships.listPendingIncoming(TARGET)).thenReturn(List.of(
                new Friendship(REQUESTER, TARGET, FriendshipStatus.PENDING, requestedAt)));
        when(profiles.findAllById(List.of(REQUESTER))).thenReturn(List.of(requesterProfile()));

        var summaries = service.listPendingIncomingRequests(TARGET);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).userId()).isEqualTo(REQUESTER);
        assertThat(summaries.get(0).username()).isEqualTo("requester");
        assertThat(summaries.get(0).requestedAt()).isEqualTo(requestedAt);
    }

    @Test
    @DisplayName("reject deletes a pending request from requester to accepter")
    void reject_withPendingRequest_deletesEdge() {
        when(friendships.find(REQUESTER, TARGET)).thenReturn(Optional.of(
                new Friendship(REQUESTER, TARGET, FriendshipStatus.PENDING, Instant.now())));

        service.reject(TARGET, REQUESTER);

        verify(friendships).delete(REQUESTER, TARGET);
    }

    @Test
    @DisplayName("removeFriend deletes accepted edges in both directions")
    void removeFriend_withAccepted_deletesBothEdges() {
        when(friendships.find(TARGET, REQUESTER)).thenReturn(Optional.of(
                new Friendship(TARGET, REQUESTER, FriendshipStatus.ACCEPTED, Instant.now())));

        service.removeFriend(TARGET, REQUESTER);

        verify(friendships).delete(TARGET, REQUESTER);
        verify(friendships).delete(REQUESTER, TARGET);
    }
}
