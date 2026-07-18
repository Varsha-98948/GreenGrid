package com.greengrid.controller;

import com.greengrid.dto.common.ApiResponse;
import com.greengrid.dto.github.*;
import com.greengrid.entity.GitHubAccount;
import com.greengrid.entity.GitRepository;
import com.greengrid.github.GitHubApiClient;
import com.greengrid.security.JwtService;
import com.greengrid.security.UserPrincipal;
import com.greengrid.service.AuthService;
import com.greengrid.service.GitHubAccountService;
import com.greengrid.service.RepositoryService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class GitHubController {

    private final GitHubAccountService gitHubAccountService;
    private final RepositoryService repositoryService;
    private final JwtService jwtService;

    @Value("${greengrid.frontend-url}")
    private String frontendUrl;

    public GitHubController(GitHubAccountService gitHubAccountService, RepositoryService repositoryService,
                             JwtService jwtService) {
        this.gitHubAccountService = gitHubAccountService;
        this.repositoryService = repositoryService;
        this.jwtService = jwtService;
    }

    // --- OAuth: connecting GitHub to an already-authenticated GreenGrid session ---

    @GetMapping("/api/github/oauth/connect-url")
    public ApiResponse<AuthorizeUrlResponse> getConnectUrl(@AuthenticationPrincipal UserPrincipal principal) {
        String state = jwtService.generateOAuthStateToken(principal.getId());
        return ApiResponse.ok(new AuthorizeUrlResponse(gitHubAccountService.buildAuthorizeUrl(state)));
    }

    // --- OAuth: "Sign in with GitHub" with no existing session ---

    @GetMapping("/api/auth/github/login-url")
    public ApiResponse<AuthorizeUrlResponse> getLoginUrl() {
        String state = jwtService.generateOAuthStateToken(null);
        return ApiResponse.ok(new AuthorizeUrlResponse(gitHubAccountService.buildAuthorizeUrl(state)));
    }

    /**
     * Single callback for both flows. GitHub redirects the raw browser here
     * (no Authorization header available), so which flow applies is
     * determined entirely by whether the signed state token carries a
     * subject (connect) or not (login/sign-up). On completion we redirect
     * back to the frontend with either a status flag (connect) or a
     * one-time token pair in the URL fragment (login) for the SPA to pick up.
     */
    @GetMapping("/api/github/oauth/callback")
    public void callback(@RequestParam String code, @RequestParam String state,
                          HttpServletResponse response) throws IOException {

        var actingUserId = jwtService.validateOAuthStateAndGetUserId(state);

        if (actingUserId.isPresent()) {
            gitHubAccountService.connectAccountForUser(actingUserId.get(), code);
            response.sendRedirect(frontendUrl + "/onboarding.html?github=connected");
        } else {
            // Login/sign-up flow — exchange happens twice would waste GitHub's
            // rate limit, so route sign-in through the same account service
            // using the same authorization code exchange internally.
            var user = gitHubAccountService.loginOrRegisterWithGitHub(code);
            String access = jwtService.generateAccessToken(user.getId(), user.getEmail());
            String refresh = jwtService.generateRefreshToken(user.getId(), user.getEmail());
            response.sendRedirect(frontendUrl + "/auth-callback.html#accessToken=" + access
                    + "&refreshToken=" + refresh + "&onboardingCompleted=" + user.isOnboardingCompleted());
        }
    }

    // --- Connection status ---

    @GetMapping("/api/github/status")
    public ApiResponse<GitHubConnectionStatus> status(@AuthenticationPrincipal UserPrincipal principal) {
        if (!gitHubAccountService.isConnected(principal.getId())) {
            return ApiResponse.ok(new GitHubConnectionStatus(false, null, null));
        }
        GitHubAccount account = gitHubAccountService.getAccountForUser(principal.getId());
        return ApiResponse.ok(new GitHubConnectionStatus(true, account.getGithubUsername(), account.getGithubAvatarUrl()));
    }

    // --- Repository selection (onboarding + Settings > Change Repository) ---

    @GetMapping("/api/github/repos")
    public ApiResponse<List<RepoSummary>> listRepos(@AuthenticationPrincipal UserPrincipal principal) {
        List<RepoSummary> repos = repositoryService.listAvailableRepositories(principal.getId()).stream()
                .map(r -> new RepoSummary(r.id(), r.name(), r.owner().login(), r.full_name(), r.isPrivate(), r.html_url()))
                .toList();
        return ApiResponse.ok(repos);
    }

    @PostMapping("/api/github/repos/create")
    public ApiResponse<RepositoryStatusResponse> createRepo(@AuthenticationPrincipal UserPrincipal principal,
                                                             @Valid @RequestBody CreateRepoRequest request) {
        GitRepository repo = repositoryService.createNewRepository(principal.getId(), request.name(), request.isPrivate());
        return ApiResponse.ok("Repository created and connected", toStatus(repo));
    }

    @PostMapping("/api/github/repos/use-existing")
    public ApiResponse<RepositoryStatusResponse> useExisting(@AuthenticationPrincipal UserPrincipal principal,
                                                              @Valid @RequestBody UseExistingRepoRequest request) {
        GitRepository repo = repositoryService.useExistingRepository(principal.getId(), request.owner(), request.name());
        return ApiResponse.ok("Repository connected", toStatus(repo));
    }

    @GetMapping("/api/github/repos/active")
    public ApiResponse<RepositoryStatusResponse> activeRepo(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(toStatus(repositoryService.getActiveRepository(principal.getId())));
    }

    private RepositoryStatusResponse toStatus(GitRepository repo) {
        return new RepositoryStatusResponse(repo.getOwner(), repo.getName(), repo.getFullName(), repo.getHtmlUrl(),
                repo.isPrivate(), repo.isWasCreatedByGreenGrid(), repo.getLastSyncStatus(), repo.getLastSyncedAt());
    }
}
