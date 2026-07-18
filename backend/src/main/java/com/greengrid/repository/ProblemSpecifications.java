package com.greengrid.repository;

import com.greengrid.entity.Difficulty;
import com.greengrid.entity.Problem;
import com.greengrid.entity.Tag;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Each method returns a Specification for exactly one optional filter.
 * ProblemService composes only the ones the caller actually supplied
 * (non-null/non-blank), so a search with just "difficulty=EASY" doesn't
 * pay for four other unnecessary WHERE clauses.
 */
public final class ProblemSpecifications {

    private ProblemSpecifications() {}

    public static Specification<Problem> belongsToUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Problem> titleContains(String title) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Problem> hasDifficulty(Difficulty difficulty) {
        return (root, query, cb) -> cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Problem> hasLanguage(String language) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("language")), language.toLowerCase());
    }

    public static Specification<Problem> hasPlatform(String platform) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("platform")), platform.toLowerCase());
    }

    public static Specification<Problem> solvedOn(LocalDate date) {
        return (root, query, cb) -> cb.equal(root.get("solvedDate"), date);
    }

    public static Specification<Problem> hasTopic(String topicName) {
        return (root, query, cb) -> {
            query.distinct(true);
            var tagJoin = root.join("tags");
            return cb.equal(cb.lower(tagJoin.get("name")), topicName.toLowerCase());
        };
    }
}
