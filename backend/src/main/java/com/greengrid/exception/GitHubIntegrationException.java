package com.greengrid.exception;

/**
 * Thrown when a call to the GitHub API fails (auth expired, rate-limited,
 * repo not found, commit conflict, etc). Kept distinct from generic 5xx
 * errors so the frontend can show a specific "reconnect GitHub" /
 * "sync failed" message rather than a generic server error.
 */
public class GitHubIntegrationException extends RuntimeException {

    private final int githubStatusCode;

    public GitHubIntegrationException(String message, int githubStatusCode) {
        super(message);
        this.githubStatusCode = githubStatusCode;
    }

    public GitHubIntegrationException(String message, int githubStatusCode, Throwable cause) {
        super(message, cause);
        this.githubStatusCode = githubStatusCode;
    }

    public int getGithubStatusCode() {
        return githubStatusCode;
    }
}
