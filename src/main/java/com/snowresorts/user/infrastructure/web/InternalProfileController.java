package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.application.FriendshipService;
import com.snowresorts.user.application.ProfileService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service endpoints protected by {@link InternalApiSecretFilter} (shared secret header).
 * Not intended for mobile clients.
 */
@RestController
@RequestMapping("/snow-resort-service/v1/users/internal")
public class InternalProfileController {

    private final ProfileService profileService;
    private final FriendshipService friendshipService;

    public InternalProfileController(ProfileService profileService, FriendshipService friendshipService) {
        this.profileService = profileService;
        this.friendshipService = friendshipService;
    }

    @GetMapping("/usernames/{username}/available")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ensureUsernameAvailable(@PathVariable String username) {
        profileService.ensureUsernameAvailable(username);
    }

    @PostMapping("/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse bootstrapProfile(@Valid @RequestBody BootstrapProfileRequest request) {
        return ProfileResponse.from(profileService.bootstrapProfile(
                request.userId(), request.email(), request.username(), request.displayName()));
    }

    @PostMapping("/profiles/summaries")
    public ProfileSummariesResponse listSummaries(@Valid @RequestBody ProfileSummariesRequest request) {
        return new ProfileSummariesResponse(profileService.listSummaries(request.userIds()));
    }

    @GetMapping("/friends/{userId}/accepted-ids")
    public FriendIdsResponse listAcceptedFriendIds(@PathVariable UUID userId) {
        return new FriendIdsResponse(friendshipService.listAcceptedFriendIds(userId));
    }

    @GetMapping("/access/stats")
    public StatsAccessResponse canViewStats(@RequestParam UUID viewerId, @RequestParam UUID ownerId) {
        return new StatsAccessResponse(profileService.canViewStats(viewerId, ownerId));
    }

    public record FriendIdsResponse(List<UUID> friendIds) {
    }

    public record StatsAccessResponse(boolean allowed) {
    }
}
