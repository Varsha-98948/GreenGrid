package com.greengrid.service;

import com.greengrid.dto.settings.AccountSummaryResponse;
import com.greengrid.dto.settings.ExportDataResponse;
import com.greengrid.entity.Problem;
import com.greengrid.entity.User;
import com.greengrid.exception.ResourceNotFoundException;
import com.greengrid.repository.GitHubAccountRepository;
import com.greengrid.repository.GitRepositoryRepository;
import com.greengrid.repository.ProblemRepository;
import com.greengrid.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Backs the Settings screen: reconnect/change-repo re-use the GitHub OAuth
 * and repository-selection flows already exposed by GitHubController, so
 * this service only owns the settings-specific concerns — theme, account
 * summary, full data export, and irreversible account deletion.
 */
@Service
public class SettingsService {

    private final UserRepository userRepository;
    private final GitHubAccountRepository gitHubAccountRepository;
    private final GitRepositoryRepository gitRepositoryRepository;
    private final ProblemRepository problemRepository;
    private final ProblemService problemService;

    public SettingsService(UserRepository userRepository, GitHubAccountRepository gitHubAccountRepository,
                            GitRepositoryRepository gitRepositoryRepository, ProblemRepository problemRepository,
                            ProblemService problemService) {
        this.userRepository = userRepository;
        this.gitHubAccountRepository = gitHubAccountRepository;
        this.gitRepositoryRepository = gitRepositoryRepository;
        this.problemRepository = problemRepository;
        this.problemService = problemService;
    }

    @Transactional
    public void updateTheme(UUID userId, String theme) {
        User user = getUser(userId);
        user.setThemePreference(theme);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AccountSummaryResponse getAccountSummary(UUID userId) {
        User user = getUser(userId);

        var githubAccount = gitHubAccountRepository.findByUserId(userId).orElse(null);
        var repo = gitRepositoryRepository.findByUserId(userId).orElse(null);

        return new AccountSummaryResponse(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getThemePreference(),
                githubAccount != null, githubAccount != null ? githubAccount.getGithubUsername() : null,
                repo != null, repo != null ? repo.getFullName() : null
        );
    }

    @Transactional(readOnly = true)
    public ExportDataResponse exportData(UUID userId) {
        User user = getUser(userId);

        var allProblems = problemRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged(Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(problemService::toResponse)
                .getContent();

        return new ExportDataResponse(user.getEmail(), user.getDisplayName(), Instant.now(),
                allProblems.size(), allProblems);
    }

    /**
     * Deletes the GreenGrid account and every row tied to it. This never
     * touches the user's actual GitHub repository or its commit
     * history — GreenGrid only forgets the user, it doesn't undo the real
     * work already pushed to GitHub, which is the entire point of the product.
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = getUser(userId);
        userRepository.delete(user); // cascades via FK ON DELETE CASCADE
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
