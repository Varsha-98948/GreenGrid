package com.greengrid.dto.settings;

import java.util.UUID;

public record AccountSummaryResponse(
        UUID userId,
        String email,
        String displayName,
        String themePreference,
        boolean githubConnected,
        String githubUsername,
        boolean repositoryConnected,
        String repositoryFullName
) {
}
