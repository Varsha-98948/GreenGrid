package com.greengrid.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * When the user pastes a leetcode.com problem URL, this fetches the
 * canonical title, difficulty, and topic tags from LeetCode's public
 * GraphQL endpoint, so the form can be pre-filled instead of hand-typed.
 * Best-effort: any failure (network, unrecognized URL, schema change)
 * simply results in an empty Optional and the user fills the form manually.
 */
@Service
public class LeetCodeMetadataService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("leetcode\\.com/problems/([a-z0-9-]+)");

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://leetcode.com")
            .defaultHeader("Content-Type", "application/json")
            .build();

    public Optional<LeetCodeMetadata> fetchMetadata(String problemUrl) {
        if (problemUrl == null || !problemUrl.contains("leetcode.com")) {
            return Optional.empty();
        }

        Matcher matcher = SLUG_PATTERN.matcher(problemUrl);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String slug = matcher.group(1);

        try {
            String query = """
                    query questionData($titleSlug: String!) {
                      question(titleSlug: $titleSlug) {
                        title
                        difficulty
                        topicTags { name }
                      }
                    }
                    """;

            Map<String, Object> body = Map.of(
                    "query", query,
                    "variables", Map.of("titleSlug", slug)
            );

            GraphQLResponse response = webClient.post()
                    .uri("/graphql")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(GraphQLResponse.class)
                    .block();

            if (response == null || response.data() == null || response.data().question() == null) {
                return Optional.empty();
            }

            var q = response.data().question();
            List<String> topics = q.topicTags() == null ? List.of()
                    : q.topicTags().stream().map(TopicTag::name).toList();

            return Optional.of(new LeetCodeMetadata(q.title(), normalizeDifficulty(q.difficulty()), topics, slug));

        } catch (Exception ex) {
            // Best-effort enrichment only — never blocks the save workflow.
            return Optional.empty();
        }
    }

    private String normalizeDifficulty(String raw) {
        if (raw == null) return null;
        return switch (raw.trim().toLowerCase()) {
            case "easy" -> "EASY";
            case "medium" -> "MEDIUM";
            case "hard" -> "HARD";
            default -> null;
        };
    }

    public record LeetCodeMetadata(String title, String difficulty, List<String> topics, String slug) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GraphQLResponse(Data data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Data(Question question) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Question(String title, String difficulty, List<TopicTag> topicTags) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TopicTag(String name) {}
}
