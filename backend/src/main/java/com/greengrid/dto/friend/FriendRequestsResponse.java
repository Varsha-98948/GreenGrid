package com.greengrid.dto.friend;

import java.util.List;

public record FriendRequestsResponse(
        List<FriendRequestDto> incoming,
        List<FriendRequestDto> outgoing
) {
}
