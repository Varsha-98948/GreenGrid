package com.greengrid.dto.settings;

import com.greengrid.dto.problem.ProblemResponse;

import java.time.Instant;
import java.util.List;

public record ExportDataResponse(
        String email,
        String displayName,
        Instant exportedAt,
        long totalProblems,
        List<ProblemResponse> problems
) {
}
