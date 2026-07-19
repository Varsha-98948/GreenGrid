package com.greengrid.github;

import com.greengrid.entity.GitRepository;
import com.greengrid.entity.Problem;
import com.greengrid.service.GitHubAccountService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Turns a saved Problem into exactly one real commit in the user's
 * repository, containing Solution.<ext>, README.md, and metadata.json
 * together under {Difficulty}/{Problem Title}/.
 *
 * Uses the Git Data API (blob -> tree -> commit -> ref) rather than three
 * separate Contents API PUTs, because each Contents API PUT is its own
 * atomic commit — three files would otherwise show up as three commits
 * with the same message, which misrepresents the history. This way,
 * "Solve: Two Sum" is one commit touching three files, same as if the
 * user had run `git add . && git commit` locally.
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
     * problem as a single commit. Returns the new commit's sha, which is
     * stored on the Problem for "recent commits" display and re-sync detection.
     */
    public String commitProblem(UUID userId, GitRepository repository, Problem problem) {
        String token = gitHubAccountService.getDecryptedTokenForUser(userId);
        String owner = repository.getOwner();
        String repoName = repository.getName();
        String branch = repository.getDefaultBranch();
        String folder = buildFolderPath(problem);
        String commitMessage = readmeGeneratorService.buildCommitMessage(problem);
        String extension = LanguageExtensionMapper.extensionFor(problem.getLanguage());

        // 1. Where does the branch currently point? (null => brand new, empty repo)
        String parentCommitSha = apiClient.getBranchTipCommitSha(token, owner, repoName, branch);
        String baseTreeSha = parentCommitSha != null
                ? apiClient.getTreeShaForCommit(token, owner, repoName, parentCommitSha)
                : null;

        // 2. Upload each file's content as a blob.
        String solutionBlobSha = apiClient.createBlob(token, owner, repoName, problem.getCode());
        String readmeBlobSha = apiClient.createBlob(token, owner, repoName, readmeGeneratorService.generate(problem));
        String metadataBlobSha = apiClient.createBlob(token, owner, repoName, metadataGeneratorService.generate(problem));

        // 3. One new tree layering all three blobs on top of the branch's current tree.
        String newTreeSha = apiClient.createTree(token, owner, repoName, baseTreeSha, List.of(
                new GitHubApiClient.TreeEntryInput(folder + "/Solution." + extension, solutionBlobSha),
                new GitHubApiClient.TreeEntryInput(folder + "/README.md", readmeBlobSha),
                new GitHubApiClient.TreeEntryInput(folder + "/metadata.json", metadataBlobSha)
        ));

        // 4. One commit pointing at that tree.
        GitHubApiClient.GitCommitObject commit =
                apiClient.createCommit(token, owner, repoName, commitMessage, newTreeSha, parentCommitSha);

        // 5. Move the branch to point at the new commit (or create the branch, first commit only).
        if (parentCommitSha != null) {
            apiClient.updateBranchRef(token, owner, repoName, branch, commit.sha());
        } else {
            apiClient.createBranchRef(token, owner, repoName, branch, commit.sha());
        }

        problem.setRepoFolderPath(folder);
        return commit.sha();
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
