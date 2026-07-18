package com.greengrid.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * A single solved coding problem: the record stored in GreenGrid's own
 * database, plus the pointer to exactly where it lives in the user's
 * GitHub repository (repoFilePath / lastCommitSha) once the save workflow
 * has pushed it. A Problem always belongs to exactly one User — there is
 * no sharing, no cross-tenant visibility, by construction.
 */
@Entity
@Table(name = "problems", indexes = {
        @jakarta.persistence.Index(name = "idx_problem_user", columnList = "user_id"),
        @jakarta.persistence.Index(name = "idx_problem_user_difficulty", columnList = "user_id,difficulty"),
        @jakarta.persistence.Index(name = "idx_problem_user_solved_date", columnList = "user_id,solved_date")
})
@Getter
@Setter
@NoArgsConstructor
public class Problem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false)
    private String title;

    @Column(name = "problem_url")
    private String problemUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "time_complexity")
    private String timeComplexity;

    @Column(name = "space_complexity")
    private String spaceComplexity;

    @Column(name = "solved_date", nullable = false)
    private LocalDate solvedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "revision_status", nullable = false)
    private RevisionStatus revisionStatus = RevisionStatus.NONE;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite = false;

    // --- GitHub sync pointers, populated once the commit/push succeeds ---

    @Column(name = "repo_folder_path")
    private String repoFolderPath;

    @Column(name = "last_commit_sha")
    private String lastCommitSha;

    @Column(name = "commit_status", nullable = false)
    private String commitStatus = "PENDING";

    // --- LeetCode auto-fetched metadata (nullable; only populated when a
    //     LeetCode URL was supplied and metadata was successfully fetched) ---

    @Column(name = "external_slug")
    private String externalSlug;

    @Column(name = "external_metadata_fetched", nullable = false)
    private boolean externalMetadataFetched = false;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "problem_tags",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"problem_id", "tag_id"})
    )
    private Set<Tag> tags = new HashSet<>();
}
