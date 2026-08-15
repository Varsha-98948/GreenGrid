package com.greengrid.dto.problem;

import com.greengrid.entity.Difficulty;
import com.greengrid.entity.RevisionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProblemResponse(
        UUID id,
        String platform,
        String title,
        String problemUrl,
        Difficulty difficulty,
        List<String> topics,
        String language,
        String code,
        String notes,
        String timeComplexity,
        String spaceComplexity,
        LocalDate solvedDate,
        RevisionStatus revisionStatus,
        boolean favorite,
        String repoFolderPath,
        String lastCommitSha,
        String commitStatus,
        Instant createdAt,
        int revisionCount,
        List<ProblemRevisionResponse> revisions
) {
    public ProblemResponse(
            UUID id, String platform, String title, String problemUrl, Difficulty difficulty,
            List<String> topics, String language, String code, String notes, String timeComplexity,
            String spaceComplexity, LocalDate solvedDate, RevisionStatus revisionStatus, boolean favorite,
            String repoFolderPath, String lastCommitSha, String commitStatus, Instant createdAt
    ) {
        this(id, platform, title, problemUrl, difficulty, topics, language, code, notes, timeComplexity,
                spaceComplexity, solvedDate, revisionStatus, favorite, repoFolderPath, lastCommitSha,
                commitStatus, createdAt, 1, List.of());
    }
}

