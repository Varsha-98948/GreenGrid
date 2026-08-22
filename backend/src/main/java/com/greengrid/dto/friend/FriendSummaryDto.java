package com.greengrid.dto.friend;

import java.time.Instant;
import java.util.UUID;

public record FriendSummaryDto(
        UUID userId,
        String displayName,
        String avatarUrl,
        String githubUsername,
        Instant friendsSince
) {
}
