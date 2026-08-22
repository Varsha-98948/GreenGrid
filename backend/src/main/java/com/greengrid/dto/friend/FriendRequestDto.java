package com.greengrid.dto.friend;

import com.greengrid.entity.FriendRequestStatus;
import java.time.Instant;
import java.util.UUID;

public record FriendRequestDto(
        UUID id,
        PublicUserSummaryDto requester,
        PublicUserSummaryDto addressee,
        FriendRequestStatus status,
        Instant createdAt
) {
}
