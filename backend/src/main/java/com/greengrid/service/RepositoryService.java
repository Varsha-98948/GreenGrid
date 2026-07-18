package com.greengrid.service;

import com.greengrid.entity.GitRepository;
import com.greengrid.entity.User;
import com.greengrid.exception.ResourceNotFoundException;
import com.greengrid.github.GitHubApiClient;
import com.greengrid.repository.GitRepositoryRepository;
import com.greengrid.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Handles onboarding step 2: the user picks exactly one GitHub repository
 * to be the permanent home for their solved problems, either by creating a
 * brand new one or selecting an existing repo they already own.
 */
@Service
public class RepositoryService {

    private final GitRepositoryRepository gitRepositoryRepository;
    private final UserRepository userRepository;
    private final GitHubApiClient apiClient;
    private final GitHubAccountService gitHubAccountService;

    public RepositoryService(GitRepositoryRepository gitRepositoryRepository, UserRepository userRepository,
                              GitHubApiClient apiClient, GitHubAccountService gitHubAccountService) {
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.userRepository = userRepository;
        this.apiClient = apiClient;
        this.gitHubAccountService = gitHubAccountService;
    }

    @Transactional(readOnly = true)
    public List<GitHubApiClient.GitHubRepo> listAvailableRepositories(UUID userId) {
        String token = gitHubAccountService.getDecryptedTokenForUser(userId);
        return apiClient.listRepositoriesForAuthenticatedUser(token);
    }

    @Transactional
    public GitRepository createNewRepository(UUID userId, String repoName, boolean isPrivate) {
        String token = gitHubAccountService.getDecryptedTokenForUser(userId);
        GitHubApiClient.GitHubRepo created = apiClient.createRepository(token, repoName, isPrivate);
        return persistSelectedRepository(userId, created, true);
    }

    @Transactional
    public GitRepository useExistingRepository(UUID userId, String owner, String repoName) {
        String token = gitHubAccountService.getDecryptedTokenForUser(userId);
        GitHubApiClient.GitHubRepo repo = apiClient.getRepository(token, owner, repoName);
        return persistSelectedRepository(userId, repo, false);
    }

    private GitRepository persistSelectedRepository(UUID userId, GitHubApiClient.GitHubRepo ghRepo, boolean createdByUs) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        GitRepository repository = gitRepositoryRepository.findByUserId(userId).orElseGet(GitRepository::new);
        repository.setUser(user);
        repository.setOwner(ghRepo.owner().login());
        repository.setName(ghRepo.name());
        repository.setFullName(ghRepo.full_name());
        repository.setDefaultBranch(ghRepo.default_branch() != null ? ghRepo.default_branch() : "main");
        repository.setPrivate(ghRepo.isPrivate());
        repository.setHtmlUrl(ghRepo.html_url());
        repository.setWasCreatedByGreenGrid(createdByUs);
        repository.setLastSyncStatus("READY");
        repository.setLastSyncedAt(Instant.now());

        GitRepository saved = gitRepositoryRepository.save(repository);

        user.setOnboardingCompleted(true);
        userRepository.save(user);

        return saved;
    }

    @Transactional(readOnly = true)
    public GitRepository getActiveRepository(UUID userId) {
        return gitRepositoryRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No repository selected yet. Please complete onboarding."));
    }
}
