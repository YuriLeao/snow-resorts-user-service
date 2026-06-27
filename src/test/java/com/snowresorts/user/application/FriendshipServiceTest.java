package com.snowresorts.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snowresorts.security.error.ConflictException;
import com.snowresorts.user.domain.model.Friendship;
import com.snowresorts.user.domain.model.FriendshipStatus;
import com.snowresorts.user.domain.port.Friendships;
import com.snowresorts.user.domain.port.Profiles;
import java.time.Instant;
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

    private FriendshipService service;

    @BeforeEach
    void setUp() {
        service = new FriendshipService(friendships, profiles);
    }

    @Test
    @DisplayName("request creates a PENDING edge from requester to target")
    void request_whenNoneExists_createsPending() {
        when(friendships.find(REQUESTER, TARGET)).thenReturn(Optional.empty());
        when(friendships.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));

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
}
