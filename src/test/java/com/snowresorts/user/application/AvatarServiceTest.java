package com.snowresorts.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.snowresorts.security.error.ForbiddenException;
import com.snowresorts.security.error.UnprocessableEntityException;
import com.snowresorts.user.domain.model.Profile;
import com.snowresorts.user.domain.model.ShareLevel;
import com.snowresorts.user.domain.port.ObjectStorage;
import com.snowresorts.user.domain.port.Profiles;
import java.time.Duration;
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
class AvatarServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String CURRENT_KEY = "avatars/" + USER_ID + "/current.webp";

    @Mock
    private Profiles profiles;
    @Mock
    private ObjectStorage objectStorage;

    private AvatarService service;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties(
                "snow-resorts-assets", "http://localhost:9000/snow-resorts-assets",
                Duration.ofMinutes(10), 2_097_152);
        service = new AvatarService(profiles, objectStorage, properties);
    }

    private Profile existingProfile() {
        Instant now = Instant.now();
        return new Profile(USER_ID, "Rider", null, null, null, null,
                ShareLevel.FRIENDS, ShareLevel.FRIENDS, now, now);
    }

    @Test
    @DisplayName("createUploadUrl returns the caller's canonical avatar key and a presigned URL")
    void createUploadUrl_forCurrentUser_returnsKeyAndPresignedUrl() {
        // Arrange
        when(objectStorage.presignedPutUrl(eqKey(), anyString(), any(Duration.class)))
                .thenReturn("https://s3.local/presigned-put");

        // Act
        AvatarUploadUrl result = service.createUploadUrl(USER_ID);

        // Assert
        assertThat(result.avatarS3Key()).isEqualTo(CURRENT_KEY);
        assertThat(result.uploadUrl()).isEqualTo("https://s3.local/presigned-put");
        assertThat(result.expiresInSeconds()).isEqualTo(600);
    }

    @Test
    @DisplayName("confirm with an existing object within the size limit updates the profile avatar")
    void confirm_withValidObject_updatesProfileWithVersionedUrl() {
        // Arrange
        when(objectStorage.objectSize(CURRENT_KEY)).thenReturn(Optional.of(1_024L));
        when(objectStorage.publicUrl(CURRENT_KEY))
                .thenReturn("http://localhost:9000/snow-resorts-assets/" + CURRENT_KEY);
        when(profiles.findById(USER_ID)).thenReturn(Optional.of(existingProfile()));
        when(profiles.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Profile updated = service.confirm(USER_ID, CURRENT_KEY);

        // Assert
        assertThat(updated.avatarS3Key()).isEqualTo(CURRENT_KEY);
        assertThat(updated.avatarUrl()).contains("?v=");
        assertThat(updated.avatarUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("confirm when the object does not exist returns 422 Unprocessable Entity")
    void confirm_withMissingObject_throwsUnprocessableEntity() {
        when(objectStorage.objectSize(CURRENT_KEY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(USER_ID, CURRENT_KEY))
                .isInstanceOf(UnprocessableEntityException.class);

        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("confirm with an oversize object returns 422 Unprocessable Entity")
    void confirm_withOversizeObject_throwsUnprocessableEntity() {
        when(objectStorage.objectSize(CURRENT_KEY)).thenReturn(Optional.of(2_097_153L));

        assertThatThrownBy(() -> service.confirm(USER_ID, CURRENT_KEY))
                .isInstanceOf(UnprocessableEntityException.class);

        verify(profiles, never()).save(any());
    }

    @Test
    @DisplayName("confirm with a key that does not belong to the caller returns 403 Forbidden")
    void confirm_withForeignKey_throwsForbidden() {
        String foreignKey = "avatars/" + OTHER_ID + "/current.webp";

        assertThatThrownBy(() -> service.confirm(USER_ID, foreignKey))
                .isInstanceOf(ForbiddenException.class);

        verify(objectStorage, never()).objectSize(anyString());
        verify(profiles, never()).save(any());
    }

    private static String eqKey() {
        return org.mockito.ArgumentMatchers.eq(CURRENT_KEY);
    }
}
