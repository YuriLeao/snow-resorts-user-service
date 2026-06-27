package com.snowresorts.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.snowresorts.user.domain.model.Friendship;
import com.snowresorts.user.domain.model.FriendshipStatus;
import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.model.ShareLevel;
import com.snowresorts.user.domain.port.Friendships;
import com.snowresorts.user.domain.port.Profiles;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    private static final UUID CALLER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TARGET = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private Profiles profiles;
    @Mock
    private Friendships friendships;

    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(profiles, friendships);
    }

    private Profile fullProfile(UUID userId) {
        Instant now = Instant.now();
        return new Profile(userId, "Rider", "avatars/" + userId + "/current.webp",
                "http://cdn/avatar?v=1", now, UUID.randomUUID(),
                ShareLevel.FRIENDS, ShareLevel.FRIENDS, now, now);
    }

    @Test
    @DisplayName("getUser for a non-friend hides the avatar URL and stats fields")
    void getUser_forNonFriend_hidesAvatarAndStats() {
        when(profiles.findById(TARGET)).thenReturn(Optional.of(fullProfile(TARGET)));
        when(friendships.find(CALLER, TARGET)).thenReturn(Optional.empty());

        Profile view = service.getUser(CALLER, TARGET);

        assertThat(view.displayName()).isEqualTo("Rider");
        assertThat(view.avatarUrl()).isNull();
        assertThat(view.shareStats()).isNull();
        assertThat(view.lastResortId()).isNull();
    }

    @Test
    @DisplayName("getUser for an accepted friend includes the avatar URL and stats")
    void getUser_forAcceptedFriend_includesAvatarAndStats() {
        when(profiles.findById(TARGET)).thenReturn(Optional.of(fullProfile(TARGET)));
        when(friendships.find(CALLER, TARGET)).thenReturn(Optional.of(
                new Friendship(CALLER, TARGET, FriendshipStatus.ACCEPTED, Instant.now())));

        Profile view = service.getUser(CALLER, TARGET);

        assertThat(view.avatarUrl()).isEqualTo("http://cdn/avatar?v=1");
        assertThat(view.shareStats()).isEqualTo(ShareLevel.FRIENDS);
    }

    @Test
    @DisplayName("getUser for self includes the avatar URL and stats without a friendship lookup")
    void getUser_forSelf_includesAvatarAndStats() {
        when(profiles.findById(CALLER)).thenReturn(Optional.of(fullProfile(CALLER)));

        Profile view = service.getUser(CALLER, CALLER);

        assertThat(view.avatarUrl()).isEqualTo("http://cdn/avatar?v=1");
    }

    @Test
    @DisplayName("getMyProfile bootstraps a default profile when none exists yet")
    void getMyProfile_withoutExistingProfile_createsDefault() {
        when(profiles.findById(CALLER)).thenReturn(Optional.empty());
        when(profiles.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile profile = service.getMyProfile(CALLER);

        assertThat(profile.userId()).isEqualTo(CALLER);
        assertThat(profile.displayName()).isEqualTo("User11111111");
        assertThat(profile.shareStats()).isEqualTo(ShareLevel.FRIENDS);
    }

    @Test
    @DisplayName("getMyProfile returns an existing profile without creating a duplicate")
    void getMyProfile_withExistingProfile_returnsExisting() {
        Profile existing = fullProfile(CALLER);
        when(profiles.findById(CALLER)).thenReturn(Optional.of(existing));

        Profile profile = service.getMyProfile(CALLER);

        assertThat(profile).isSameAs(existing);
    }

    @Test
    @DisplayName("updateMyProfile persists the new display name and share levels")
    void updateMyProfile_withNewValues_persistsChanges() {
        when(profiles.findById(CALLER)).thenReturn(Optional.of(fullProfile(CALLER)));
        when(profiles.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile updated = service.updateMyProfile(CALLER, "Snowy", ShareLevel.NOBODY, ShareLevel.NOBODY);

        assertThat(updated.displayName()).isEqualTo("Snowy");
        assertThat(updated.shareStats()).isEqualTo(ShareLevel.NOBODY);
        assertThat(updated.shareLocation()).isEqualTo(ShareLevel.NOBODY);
    }

    @Test
    @DisplayName("bootstrapProfile creates a default profile with sanitized email local-part as display name")
    void bootstrapProfile_withNewUser_createsDefaultProfile() {
        when(profiles.findById(CALLER)).thenReturn(Optional.empty());
        when(profiles.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile created = service.bootstrapProfile(CALLER, "New.Rider@snow-resorts.com");

        assertThat(created.userId()).isEqualTo(CALLER);
        assertThat(created.displayName()).isEqualTo("New.Rider");
        assertThat(created.shareStats()).isEqualTo(ShareLevel.FRIENDS);
        assertThat(created.shareLocation()).isEqualTo(ShareLevel.FRIENDS);
    }

    @Test
    @DisplayName("bootstrapProfile is idempotent when a profile already exists")
    void bootstrapProfile_withExistingProfile_returnsExisting() {
        Profile existing = fullProfile(CALLER);
        when(profiles.findById(CALLER)).thenReturn(Optional.of(existing));

        Profile result = service.bootstrapProfile(CALLER, "ignored@snow-resorts.com");

        assertThat(result).isSameAs(existing);
    }

    @Test
    @DisplayName("defaultDisplayName falls back to User prefix when email local-part is blank after sanitization")
    void defaultDisplayName_withUnsanitizableEmail_usesUserPrefix() {
        assertThat(ProfileService.defaultDisplayName("@@@", CALLER)).isEqualTo("User11111111");
    }
}
