package com.snowresorts.user.infrastructure.web;

import com.snowresorts.security.SecurityUtils;
import com.snowresorts.user.application.FriendshipService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Friend list and friend-request endpoints. The acting user is always the JWT {@code sub}. */
@RestController
@RequestMapping("/snow-resort-service/v1/friends")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping
    public List<FriendSummary> listFriends() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return friendshipService.listFriendSummaries(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public void requestFriend(@Valid @RequestBody FriendRequestRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        friendshipService.requestByUsername(userId, request.friendUsername());
    }

    @PostMapping("/requests/{requesterId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptRequest(@PathVariable UUID requesterId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        friendshipService.accept(userId, requesterId);
    }

    @GetMapping("/requests/incoming")
    public List<FriendRequestSummary> listIncomingRequests() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return friendshipService.listPendingIncomingRequests(userId);
    }

    @PostMapping("/requests/{requesterId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectRequest(@PathVariable UUID requesterId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        friendshipService.reject(userId, requesterId);
    }

    @DeleteMapping("/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(@PathVariable UUID friendId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        friendshipService.removeFriend(userId, friendId);
    }
}
