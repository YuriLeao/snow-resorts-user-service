package com.snowresorts.user.infrastructure.web;

import com.snowresorts.user.application.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public InternalProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping("/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse bootstrapProfile(@Valid @RequestBody BootstrapProfileRequest request) {
        return ProfileResponse.from(profileService.bootstrapProfile(request.userId(), request.email()));
    }
}
