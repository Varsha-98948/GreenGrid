package com.greengrid.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.greengrid.exception.GitHubIntegrationException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * All outbound calls to the GitHub REST API live here. Every method takes
 * the caller's own decrypted access token — this class never holds a
 * token itself, keeping it stateless and safe to reuse across users.
 */
@Component
public class GitHubApiClient {

    private final WebClient webClient;

    public GitHubApiClient(WebClient githubWebClient) {
        this.webClient = githubWebClient;
    }

    public GitHubUser getAuthenticatedUser(String accessToken) {
        return call(() -> webClient.get()
                .uri("/user")
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GitHubUser.class)
                .block());
    }

    public List<GitHubRepo> listRepositoriesForAuthenticatedUser(String accessToken) {
        return call(() -> webClient.get()
                .uri(uri -> uri.path("/user/repos")
                        .queryParam("sort", "updated")
                        .queryParam("per_page", 100)
                        .queryParam("affiliation", "owner")
                        .build())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToFlux(GitHubRepo.class)
                .collectList()
                .block());
    }

    public GitHubRepo getRepository(String accessToken, String owner, String repo) {
        return call(() -> webClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GitHubRepo.class)
                .block());
    }

    public GitHubRepo createRepository(String accessToken, String name, boolean isPrivate) {
        Map<String, Object> body = Map.of(
                "name", name,
                "private", isPrivate,
                "auto_init", true,
                "description", "Coding problems solved and tracked with GreenGrid"
        );
        return call(() -> webClient.post()
                .uri("/user/repos")
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GitHubRepo.class)
                .block());
    }

    /**
     * Creates or updates a single file via the Contents API — this *is* the
     * commit. sha must be supplied (the blob's current sha) when updating an
     * existing file; omit/null when creating a new file for the first time.
     */
    public GitHubCommitResult createOrUpdateFile(String accessToken, String owner, String repo,
                                                  String path, String content, String commitMessage,
                                                  String branch, String existingFileSha) {
        String encodedContent = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));

        var bodyBuilder = new java.util.HashMap<String, Object>();
        bodyBuilder.put("message", commitMessage);
        bodyBuilder.put("content", encodedContent);
        bodyBuilder.put("branch", branch);
        if (existingFileSha != null) {
            bodyBuilder.put("sha", existingFileSha);
        }

        return call(() -> webClient.put()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(bodyBuilder)
                .retrieve()
                .bodyToMono(GitHubCommitResult.class)
                .block());
    }

    /** Returns the current sha of a file if it exists, or null if it doesn't (404). */
    public String getFileShaIfExists(String accessToken, String owner, String repo, String path, String branch) {
        try {
            GitHubContentMeta meta = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/repos/{owner}/{repo}/contents/{path}")
                            .queryParam("ref", branch)
                            .build(owner, repo, path))
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(GitHubContentMeta.class)
                    .block();
            return meta != null ? meta.sha() : null;
        } catch (WebClientResponseException.NotFound notFound) {
            return null;
        } catch (WebClientResponseException ex) {
            throw new GitHubIntegrationException("Failed to check existing file: " + ex.getMessage(), ex.getStatusCode().value(), ex);
        }
    }

    // --- Git Data API: blob -> tree -> commit -> ref, used to land several
    //     files in exactly one commit instead of one commit per file. -------

    /** The current tip commit sha of a branch, or null if the branch has no commits yet (empty repo). */
    public String getBranchTipCommitSha(String accessToken, String owner, String repo, String branch) {
        try {
            GitRef ref = webClient.get()
                    .uri("/repos/{owner}/{repo}/git/ref/heads/{branch}", owner, repo, branch)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(GitRef.class)
                    .block();
            return ref != null && ref.object() != null ? ref.object().sha() : null;
        } catch (WebClientResponseException.NotFound notFound) {
            return null;
        } catch (WebClientResponseException ex) {
            throw new GitHubIntegrationException("Failed to read branch ref: " + ex.getMessage(), ex.getStatusCode().value(), ex);
        }
    }

    /** The tree sha that a given commit points at — the "base_tree" for a new tree. */
    public String getTreeShaForCommit(String accessToken, String owner, String repo, String commitSha) {
        return call(() -> {
            GitCommitObject commit = webClient.get()
                    .uri("/repos/{owner}/{repo}/git/commits/{sha}", owner, repo, commitSha)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(GitCommitObject.class)
                    .block();
            return commit != null && commit.tree() != null ? commit.tree().sha() : null;
        });
    }

    public String createBlob(String accessToken, String owner, String repo, String content) {
        String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> body = Map.of("content", encoded, "encoding", "base64");
        return call(() -> {
            GitBlobResult result = webClient.post()
                    .uri("/repos/{owner}/{repo}/git/blobs", owner, repo)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(GitBlobResult.class)
                    .block();
            return result != null ? result.sha() : null;
        });
    }

    /**
     * Creates a new tree containing the given path->blobSha entries, layered
     * on top of baseTreeSha (all files not mentioned are carried over
     * unchanged). baseTreeSha may be null for a brand new, empty repo.
     */
    public String createTree(String accessToken, String owner, String repo, String baseTreeSha,
                              List<TreeEntryInput> entries) {
        List<Map<String, Object>> treeEntries = entries.stream()
                .map(e -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("path", e.path());
                    m.put("mode", "100644");
                    m.put("type", "blob");
                    m.put("sha", e.blobSha());
                    return m;
                })
                .toList();

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("tree", treeEntries);
        if (baseTreeSha != null) {
            body.put("base_tree", baseTreeSha);
        }

        return call(() -> {
            GitTreeResult result = webClient.post()
                    .uri("/repos/{owner}/{repo}/git/trees", owner, repo)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(GitTreeResult.class)
                    .block();
            return result != null ? result.sha() : null;
        });
    }

    /** parentCommitSha may be null only for the very first commit in an empty repo. */
    public GitCommitObject createCommit(String accessToken, String owner, String repo, String message,
                                         String treeSha, String parentCommitSha) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("message", message);
        body.put("tree", treeSha);
        body.put("parents", parentCommitSha != null ? List.of(parentCommitSha) : List.of());

        return call(() -> webClient.post()
                .uri("/repos/{owner}/{repo}/git/commits", owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(GitCommitObject.class)
                .block());
    }

    /** Moves an existing branch ref forward to point at newCommitSha. */
    public void updateBranchRef(String accessToken, String owner, String repo, String branch, String newCommitSha) {
        Map<String, Object> body = Map.of("sha", newCommitSha, "force", false);
        call(() -> webClient.patch()
                .uri("/repos/{owner}/{repo}/git/refs/heads/{branch}", owner, repo, branch)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block());
    }

    /** Creates a branch ref that doesn't exist yet — only needed for a repo's very first commit. */
    public void createBranchRef(String accessToken, String owner, String repo, String branch, String commitSha) {
        Map<String, Object> body = Map.of("ref", "refs/heads/" + branch, "sha", commitSha);
        call(() -> webClient.post()
                .uri("/repos/{owner}/{repo}/git/refs", owner, repo)
                .headers(h -> h.setBearerAuth(accessToken))
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block());
    }

    public record TreeEntryInput(String path, String blobSha) {}

    private <T> T call(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (WebClientResponseException ex) {
            throw new GitHubIntegrationException(
                    "GitHub API error: " + ex.getStatusCode() + " — " + ex.getResponseBodyAsString(),
                    ex.getStatusCode().value(), ex);
        }
    }

    // --- Response shapes -------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubUser(long id, String login, String name, String avatar_url) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubRepo(
            long id,
            String name,
            String full_name,
            @com.fasterxml.jackson.annotation.JsonProperty("private") boolean isPrivate,
            String html_url,
            String default_branch,
            GitHubOwner owner
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record GitHubOwner(String login) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubCommitResult(GitHubCommitInfo commit, GitHubContentMeta content) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record GitHubCommitInfo(String sha, String html_url) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubContentMeta(String sha, String path) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitRef(GitRefObject object) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record GitRefObject(String sha, String type) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitCommitObject(String sha, String html_url, GitTreeRef tree) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record GitTreeRef(String sha) {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitBlobResult(String sha) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitTreeResult(String sha) {}
}
