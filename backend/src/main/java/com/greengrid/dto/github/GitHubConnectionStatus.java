package com.greengrid.dto.github;

public record GitHubConnectionStatus(boolean connected, String githubUsername, String githubAvatarUrl) {
}
