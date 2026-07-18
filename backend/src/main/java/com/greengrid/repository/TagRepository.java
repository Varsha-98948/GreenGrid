package com.greengrid.repository;

import com.greengrid.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findByUserIdAndNameIgnoreCase(UUID userId, String name);

    List<Tag> findAllByUserId(UUID userId);
}
