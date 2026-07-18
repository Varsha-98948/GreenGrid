package com.greengrid.dto.github;

import jakarta.validation.constraints.NotBlank;

public record UseExistingRepoRequest(@NotBlank String owner, @NotBlank String name) {
}
