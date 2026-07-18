package com.greengrid.github;

import com.greengrid.entity.GitRepository;
import com.greengrid.entity.Problem;
import com.greengrid.service.GitHubAccountService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Turns a saved Problem into three real commits worth of content in the
 * user's repository: Solution.<ext>, README.md, metadata.json, all under
 * {Difficulty}/{Problem Title}/. Each file is written with its own
 * Contents API call (GitHub commits each PUT individually) using the exact
 * same commit message, so they read as one logical unit of work in the
 * repo's history.
 */
@Service
public class CommitService {

    private final GitHubApiClient apiClient;
    private final GitHubAccountService gitHubAccountService;
    private final ReadmeGeneratorService readmeGeneratorService;
    private final MetadataGeneratorService metadataGeneratorService;

    public CommitService(GitHubApiClient apiClient, GitHubAccountService gitHubAccountService,
                          ReadmeGeneratorService readmeGeneratorService,
                          MetadataGeneratorService metadataGeneratorService) {
        this.apiClient = apiClient;
        this.gitHubAccountService = gitHubAccountService;
        this.readmeGeneratorService = readmeGeneratorService;
        this.metadataGeneratorService = metadataGeneratorService;
    }

    /**
     * Commits/pushes the solution, README, and metadata for the given
     * problem into the given repository. Returns the sha of the final
     * (solution file) commit, which is stored on the Problem for later
     * "recent commits" display and re-sync detection.
     */
    public String commitProblem(UUID userId, GitRepository repository, Problem problem) {
        String token = gitHubAccountService.getDecryptedTokenForUser(userId);
        String folder = buildFolderPath(problem);
        String branch = repository.getDefaultBranch();
        String owner = repository.getOwner();
        String repoName = repository.getName();
        String commitMessage = readmeGeneratorService.buildCommitMessage(problem);

        String extension = LanguageExtensionMapper.extensionFor(problem.getLanguage());
        String solutionPath = folder + "/Solution." + extension;
        String readmePath = folder + "/README.md";
        String metadataPath = folder + "/metadata.json";

        String solutionSha = apiClient.getFileShaIfExists(token, owner, repoName, solutionPath, branch);
        apiClient.createOrUpdateFile(token, owner, repoName, solutionPath, problem.getCode(),
                commitMessage, branch, solutionSha);

        String readmeSha = apiClient.getFileShaIfExists(token, owner, repoName, readmePath, branch);
        apiClient.createOrUpdateFile(token, owner, repoName, readmePath,
                readmeGeneratorService.generate(problem), commitMessage, branch, readmeSha);

        String metadataSha = apiClient.getFileShaIfExists(token, owner, repoName, metadataPath, branch);
        var result = apiClient.createOrUpdateFile(token, owner, repoName, metadataPath,
                metadataGeneratorService.generate(problem), commitMessage, branch, metadataSha);

        problem.setRepoFolderPath(folder);

        return result.commit() != null ? result.commit().sha() : null;
    }

    /**
     * {Difficulty}/{Title}/ — title is sanitized only for filesystem-unsafe
     * characters (/, \, control chars); spaces are preserved deliberately
     * to match the required repository layout, e.g. "Easy/Two Sum/".
     */
    private String buildFolderPath(Problem problem) {
        String sanitizedTitle = problem.getTitle()
                .replaceAll("[\\\\/:*?\"<>|]", "")
                .trim();
        return problem.getDifficulty().folderName() + "/" + sanitizedTitle;
    }
}
