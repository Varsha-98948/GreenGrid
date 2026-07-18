package com.greengrid.dto.github;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateRepoRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Repository name may only contain letters, numbers, dots, hyphens and underscores")
        String name,
        boolean isPrivate
) {
}
