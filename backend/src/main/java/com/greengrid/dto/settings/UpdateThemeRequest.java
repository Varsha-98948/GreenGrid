package com.greengrid.dto.settings;

import jakarta.validation.constraints.Pattern;

public record UpdateThemeRequest(
        @Pattern(regexp = "^(dark|light)$", message = "Theme must be 'dark' or 'light'") String theme
) {
}
