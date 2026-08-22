package com.greengrid.controller;

import com.greengrid.dto.common.ApiResponse;
import com.greengrid.dto.friend.*;
import com.greengrid.security.UserPrincipal;
import com.greengrid.service.FriendService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping("/search")
    public ApiResponse<List<UserSearchResultDto>> searchUsers(@AuthenticationPrincipal UserPrincipal principal,
                                                              @RequestParam(name = "q", required = false) String query) {
        return ApiResponse.ok(friendService.searchUsers(principal.getId(), query));
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FriendRequestDto> sendFriendRequest(@AuthenticationPrincipal UserPrincipal principal,
                                                           @Valid @RequestBody SendFriendRequest request) {
        return ApiResponse.ok("Friend request sent", friendService.sendFriendRequest(principal.getId(), request.toUserId()));
    }

    @GetMapping("/requests")
    public ApiResponse<FriendRequestsResponse> getPendingRequests(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(friendService.getPendingRequests(principal.getId()));
    }

    @PostMapping("/requests/{id}/accept")
    public ApiResponse<Void> acceptFriendRequest(@AuthenticationPrincipal UserPrincipal principal,
                                                 @PathVariable UUID id) {
        friendService.acceptFriendRequest(principal.getId(), id);
        return ApiResponse.message("Friend request accepted");
    }

    @PostMapping("/requests/{id}/reject")
    public ApiResponse<Void> rejectFriendRequest(@AuthenticationPrincipal UserPrincipal principal,
                                                 @PathVariable UUID id) {
        friendService.rejectFriendRequest(principal.getId(), id);
        return ApiResponse.message("Friend request rejected");
    }

    @DeleteMapping("/requests/{id}")
    public ApiResponse<Void> cancelFriendRequest(@AuthenticationPrincipal UserPrincipal principal,
                                                 @PathVariable UUID id) {
        friendService.cancelFriendRequest(principal.getId(), id);
        return ApiResponse.message("Friend request cancelled");
    }

    @GetMapping
    public ApiResponse<List<FriendSummaryDto>> getFriends(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(friendService.getFriends(principal.getId()));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> removeFriend(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable UUID userId) {
        friendService.removeFriend(principal.getId(), userId);
        return ApiResponse.message("Friend removed");
    }

    @GetMapping("/{userId}/progress")
    public ApiResponse<FriendProgressResponse> getFriendProgress(@AuthenticationPrincipal UserPrincipal principal,
                                                                 @PathVariable UUID userId,
                                                                 @RequestParam(required = false) String timezone) {
        ZoneId zone = resolveZone(timezone);
        return ApiResponse.ok(friendService.getFriendProgress(principal.getId(), userId, zone));
    }

    private ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            return ZoneId.systemDefault();
        }
    }
}
