package com.snowresorts.user.infrastructure.web;

import com.snowresorts.security.SecurityUtils;
import com.snowresorts.user.application.AvatarService;
import com.snowresorts.user.application.ProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profile and avatar endpoints. The owner is always derived from the JWT {@code sub} (never from
 * the request body or path) so callers can only mutate their own profile/avatar (IDOR protection).
 */
@RestController
@RequestMapping("/snow-resort-service/v1/users")
public class UserController {

    private final ProfileService profileService;
    private final AvatarService avatarService;

    public UserController(ProfileService profileService, AvatarService avatarService) {
        this.profileService = profileService;
        this.avatarService = avatarService;
    }

    @GetMapping("/me")
    public ProfileResponse getMyProfile() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ProfileResponse.from(profileService.getMyProfile(userId));
    }

    @PutMapping("/me")
    public ProfileResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ProfileResponse.from(profileService.updateMyProfile(
                userId, request.displayName(), request.shareStats(), request.shareLocation()));
    }

    @PostMapping("/me/avatar/upload-url")
    public AvatarUploadUrlResponse createAvatarUploadUrl() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return AvatarUploadUrlResponse.from(avatarService.createUploadUrl(userId));
    }

    @PutMapping("/me/avatar/confirm")
    public ProfileResponse confirmAvatar(@Valid @RequestBody AvatarConfirmRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ProfileResponse.from(avatarService.confirm(userId, request.avatarS3Key()));
    }

    @DeleteMapping("/me/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAvatar() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        avatarService.delete(userId);
    }

    @GetMapping("/{id}")
    public ProfileResponse getUser(@PathVariable UUID id) {
        UUID callerId = SecurityUtils.requireCurrentUserId();
        return ProfileResponse.from(profileService.getUser(callerId, id));
    }
}
