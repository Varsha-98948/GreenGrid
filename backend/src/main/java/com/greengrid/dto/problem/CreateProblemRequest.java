package com.greengrid.dto.problem;

import com.greengrid.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateProblemRequest(
        @NotBlank String platform,
        @NotBlank String title,
        String problemUrl,
        @NotNull Difficulty difficulty,
        List<String> topics,
        @NotBlank String language,
        @NotBlank String code,
        String notes,
        String timeComplexity,
        String spaceComplexity,
        LocalDate solvedDate
) {
}
