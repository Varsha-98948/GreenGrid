package com.greengrid.dto.problem;

import com.greengrid.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record CreateRevisionRequest(
        String title,
        @NotBlank(message = "Language is required")
        String language,
        @NotBlank(message = "Code is required")
        String code,
        String notes,
        String timeComplexity,
        String spaceComplexity,
        // Optional problem metadata fields for single-request update
        String platform,
        String problemTitle,
        String problemUrl,
        Difficulty difficulty,
        List<String> topics,
        LocalDate solvedDate
) {
    public CreateRevisionRequest(
            String title, String language, String code, String notes, String timeComplexity, String spaceComplexity
    ) {
        this(title, language, code, notes, timeComplexity, spaceComplexity, null, null, null, null, null, null);
    }
}
