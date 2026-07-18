package com.greengrid.repository;

import com.greengrid.entity.GitRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GitRepositoryRepository extends JpaRepository<GitRepository, UUID> {

    Optional<GitRepository> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
