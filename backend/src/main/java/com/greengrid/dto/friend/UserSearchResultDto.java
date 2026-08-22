package com.greengrid.dto.friend;

import java.util.UUID;

public record UserSearchResultDto(
        UUID userId,
        String displayName,
        String avatarUrl,
        String githubUsername,
        String relationshipStatus
) {
}
