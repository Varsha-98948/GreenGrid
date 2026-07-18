package com.greengrid.dto.github;

import java.time.Instant;

public record RepositoryStatusResponse(
        String owner, String name, String fullName, String htmlUrl,
        boolean isPrivate, boolean wasCreatedByGreenGrid,
        String lastSyncStatus, Instant lastSyncedAt
) {
}
