package com.greengrid.dto.github;

public record RepoSummary(long id, String name, String owner, String fullName, boolean isPrivate, String htmlUrl) {
}
