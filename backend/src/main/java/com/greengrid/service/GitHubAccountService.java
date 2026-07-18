package com.greengrid.service;

import com.greengrid.entity.GitHubAccount;
import com.greengrid.entity.User;
import com.greengrid.exception.ResourceNotFoundException;
import com.greengrid.github.GitHubApiClient;
import com.greengrid.github.GitHubOAuthClient;
import com.greengrid.repository.GitHubAccountRepository;
import com.greengrid.repository.UserRepository;
import com.greengrid.security.TokenEncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Owns the lifecycle of a user's connected GitHub identity: completing the
 * OAuth exchange, persisting the encrypted token, and — the one place in
 * the codebase allowed to hand back a *decrypted* token — supplying it to
 * other services for the duration of a single outbound GitHub API call.
 */
@Service
public class GitHubAccountService {

    private final GitHubAccountRepository githubAccountRepository;
    private final UserRepository userRepository;
    private final GitHubOAuthClient oauthClient;
    private final GitHubApiClient apiClient;
    private final TokenEncryptionService tokenEncryptionService;

    public GitHubAccountService(GitHubAccountRepository githubAccountRepository, UserRepository userRepository,
                                 GitHubOAuthClient oauthClient, GitHubApiClient apiClient,
                                 TokenEncryptionService tokenEncryptionService) {
        this.githubAccountRepository = githubAccountRepository;
        this.userRepository = userRepository;
        this.oauthClient = oauthClient;
        this.apiClient = apiClient;
        this.tokenEncryptionService = tokenEncryptionService;
    }

    public String buildAuthorizeUrl(String signedState) {
        return oauthClient.buildAuthorizeUrl(signedState);
    }

    /**
     * Completes the OAuth exchange and links (or relinks) the resulting
     * GitHub identity to the given, already-authenticated GreenGrid user.
     */
    @Transactional
    public GitHubAccount connectAccountForUser(UUID greenGridUserId, String authorizationCode) {
        String accessToken = oauthClient.exchangeCodeForAccessToken(authorizationCode);
        GitHubApiClient.GitHubUser githubUser = apiClient.getAuthenticatedUser(accessToken);

        User user = userRepository.findById(greenGridUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        GitHubAccount account = githubAccountRepository.findByUserId(greenGridUserId)
                .orElseGet(GitHubAccount::new);

        account.setUser(user);
        account.setGithubUserId(githubUser.id());
        account.setGithubUsername(githubUser.login());
        account.setGithubAvatarUrl(githubUser.avatar_url());
        account.setEncryptedAccessToken(tokenEncryptionService.encrypt(accessToken));
        account.setTokenScope("repo,read:user");
        account.setConnectedAt(Instant.now());

        return githubAccountRepository.save(account);
    }

    /**
     * "Sign in with GitHub" — no pre-existing GreenGrid session. Finds the
     * user already linked to this GitHub identity, or provisions a brand
     * new account + linked GitHubAccount in one step. GitHub's primary
     * email may be private, so a deterministic, clearly-marked placeholder
     * is used as a fallback rather than leaving the required column null.
     */
    @Transactional
    public User loginOrRegisterWithGitHub(String authorizationCode) {
        String accessToken = oauthClient.exchangeCodeForAccessToken(authorizationCode);
        GitHubApiClient.GitHubUser githubUser = apiClient.getAuthenticatedUser(accessToken);

        return githubAccountRepository.findByGithubUserIdWithUser(githubUser.id())
                .map(GitHubAccount::getUser)
                .orElseGet(() -> provisionNewGitHubUser(githubUser, accessToken));
    }

    private User provisionNewGitHubUser(GitHubApiClient.GitHubUser githubUser, String accessToken) {
        User user = new User();
        user.setEmail(githubUser.login() + "+" + githubUser.id() + "@users.noreply.greengrid.dev");
        user.setDisplayName(githubUser.name() != null ? githubUser.name() : githubUser.login());
        user.setAvatarUrl(githubUser.avatar_url());
        user = userRepository.save(user);

        GitHubAccount account = new GitHubAccount();
        account.setUser(user);
        account.setGithubUserId(githubUser.id());
        account.setGithubUsername(githubUser.login());
        account.setGithubAvatarUrl(githubUser.avatar_url());
        account.setEncryptedAccessToken(tokenEncryptionService.encrypt(accessToken));
        account.setTokenScope("repo,read:user");
        account.setConnectedAt(Instant.now());
        githubAccountRepository.save(account);

        return user;
    }

    @Transactional(readOnly = true)
    public GitHubAccount getAccountForUser(UUID userId) {
        return githubAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No GitHub account connected. Please connect GitHub first."));
    }

    @Transactional(readOnly = true)
    public boolean isConnected(UUID userId) {
        return githubAccountRepository.existsByUserId(userId);
    }

    /**
     * Decrypts the stored token for exactly one outbound call. Callers must
     * not cache, log, or otherwise persist the returned value.
     */
    @Transactional(readOnly = true)
    public String getDecryptedTokenForUser(UUID userId) {
        return tokenEncryptionService.decrypt(getAccountForUser(userId).getEncryptedAccessToken());
    }
}
