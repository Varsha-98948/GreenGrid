package com.greengrid.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.greengrid.exception.GitHubIntegrationException;
import com.greengrid.security.GitHubOAuthProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * The GitHub OAuth "web application flow": exchanges a one-time
 * authorization code (received on our callback redirect) for a real
 * access token. This talks to github.com, not api.github.com, so it
 * gets its own WebClient rather than reusing {@link GitHubApiClient}'s.
 */
@Component
public class GitHubOAuthClient {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://github.com")
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build();

    private final GitHubOAuthProperties properties;

    public GitHubOAuthClient(GitHubOAuthProperties properties) {
        this.properties = properties;
    }

    public String buildAuthorizeUrl(String state) {
        return "https://github.com/login/oauth/authorize"
                + "?client_id=" + properties.clientId()
                + "&redirect_uri=" + properties.redirectUri()
                + "&scope=repo,read:user"
                + "&state=" + state;
    }

    public String exchangeCodeForAccessToken(String code) {
        Map<String, String> body = Map.of(
                "client_id", properties.clientId(),
                "client_secret", properties.clientSecret(),
                "code", code,
                "redirect_uri", properties.redirectUri()
        );

        try {
            TokenResponse response = webClient.post()
                    .uri("/login/oauth/access_token")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();

            if (response == null || response.access_token() == null) {
                throw new GitHubIntegrationException(
                        "GitHub did not return an access token" + (response != null ? ": " + response.error_description() : ""),
                        502);
            }
            return response.access_token();
        } catch (WebClientResponseException ex) {
            throw new GitHubIntegrationException("GitHub OAuth exchange failed: " + ex.getMessage(),
                    ex.getStatusCode().value(), ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(String access_token, String scope, String token_type,
                                  String error, String error_description) {}
}
