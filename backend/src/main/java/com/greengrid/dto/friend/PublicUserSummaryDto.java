package com.greengrid.dto.friend;

import java.util.UUID;

public record PublicUserSummaryDto(
        UUID userId,
        String displayName,
        String avatarUrl,
        String githubUsername
) {
}
