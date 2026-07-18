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
 * A user's connected GitHub identity and encrypted OAuth token.
 *
 * SECURITY: {@code encryptedAccessToken} holds AES-256-GCM ciphertext, never
 * a raw GitHub token. Decryption happens exclusively inside
 * {@code TokenEncryptionService}, in-memory, at the moment a GitHub API call
 * is made — the plaintext token is never logged, returned in a DTO, or
 * persisted anywhere else. One GitHub account per GreenGrid user (1:1),
 * enforced by the unique column below.
 */
@Entity
@Table(name = "github_accounts")
@Getter
@Setter
@NoArgsConstructor
public class GitHubAccount extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "github_user_id", nullable = false, unique = true)
    private Long githubUserId;

    @Column(name = "github_username", nullable = false)
    private String githubUsername;

    @Column(name = "github_avatar_url")
    private String githubAvatarUrl;

    /**
     * AES-256-GCM ciphertext (Base64), format: {nonce}:{ciphertext}.
     * See {@code com.greengrid.security.TokenEncryptionService}.
     */
    @Column(name = "encrypted_access_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "token_scope")
    private String tokenScope;

    @Column(name = "connected_at", nullable = false)
    private java.time.Instant connectedAt;
}
