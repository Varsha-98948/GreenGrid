package com.greengrid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A GreenGrid account. Authentication can originate either from
 * email/password registration or directly from GitHub OAuth
 * ("Sign in with GitHub") — githubAccount is populated once the user
 * connects GitHub, which may happen at signup or later in onboarding.
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(name = "uk_users_email", columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false)
    private String email;

    /**
     * BCrypt hash. Null for accounts that only ever authenticate via GitHub OAuth.
     */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "theme_preference", nullable = false)
    private String themePreference = "dark";
}
