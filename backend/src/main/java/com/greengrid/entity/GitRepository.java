package com.greengrid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The single GitHub repository a user has designated to hold their solved
 * problems (chosen during onboarding — either newly created or an existing
 * repo). One active repository per user; switching repos is a Settings
 * action that updates this row rather than creating a new one, so problem
 * history always points at the currently active repo.
 */
@Entity
@Table(name = "git_repositories")
@Getter
@Setter
@NoArgsConstructor
public class GitRepository extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "owner", nullable = false)
    private String owner;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "default_branch", nullable = false)
    private String defaultBranch = "main";

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate;

    @Column(name = "html_url", nullable = false)
    private String htmlUrl;

    @Column(name = "was_created_by_greengrid", nullable = false)
    private boolean wasCreatedByGreenGrid;

    @Column(name = "last_sync_status")
    private String lastSyncStatus;

    @Column(name = "last_synced_at")
    private java.time.Instant lastSyncedAt;
}
