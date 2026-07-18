package com.greengrid.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "greengrid.github")
public record GitHubOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String apiBaseUrl
) {
}
