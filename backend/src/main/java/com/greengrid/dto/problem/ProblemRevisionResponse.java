package com.greengrid.dto.problem;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProblemRevisionResponse(
        UUID id,
        int revisionNumber,
        String title,
        String language,
        String code,
        String notes,
        String timeComplexity,
        String spaceComplexity,
        String repoFolderPath,
        String lastCommitSha,
        String commitStatus,
        OffsetDateTime createdAt
) {}
