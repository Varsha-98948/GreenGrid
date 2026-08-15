package com.greengrid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single solution revision for a problem.
 * One Problem can have multiple solution revisions (e.g. Revision 1, Revision 2).
 */
@Entity
@Table(name = "problem_revisions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"problem_id", "revision_number"})
})
@Getter
@Setter
@NoArgsConstructor
public class ProblemRevision extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "title")
    private String title;

    @Column(name = "language", nullable = false)
    private String language;

    @Column(name = "code", nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "time_complexity")
    private String timeComplexity;

    @Column(name = "space_complexity")
    private String spaceComplexity;

    @Column(name = "repo_folder_path")
    private String repoFolderPath;

    @Column(name = "last_commit_sha")
    private String lastCommitSha;

    @Column(name = "commit_status", nullable = false)
    private String commitStatus = "PENDING";
}
