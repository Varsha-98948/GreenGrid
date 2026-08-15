package com.greengrid.dto.problem;

import jakarta.validation.constraints.NotBlank;

public record CreateRevisionRequest(
        String title,
        @NotBlank(message = "Language is required")
        String language,
        @NotBlank(message = "Code is required")
        String code,
        String notes,
        String timeComplexity,
        String spaceComplexity
) {}
