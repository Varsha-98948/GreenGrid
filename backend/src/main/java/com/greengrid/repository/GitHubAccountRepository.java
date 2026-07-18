package com.greengrid.repository;

import com.greengrid.entity.GitHubAccount;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GitHubAccountRepository extends JpaRepository<GitHubAccount, UUID> {

    Optional<GitHubAccount> findByUserId(UUID userId);

    Optional<GitHubAccount> findByGithubUserId(Long githubUserId);

    @Query("""
        SELECT g 
        FROM GitHubAccount g
        JOIN FETCH g.user
        WHERE g.githubUserId = :githubUserId
    """)
    Optional<GitHubAccount> findByGithubUserIdWithUser(
            @Param("githubUserId") Long githubUserId
    );

    boolean existsByUserId(UUID userId);
}