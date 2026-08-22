package com.greengrid.repository;

import com.greengrid.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE u.id != :excludeUserId AND (LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY u.displayName ASC")
    java.util.List<User> searchUsersByDisplayNameOrEmail(@org.springframework.data.repository.query.Param("query") String query, @org.springframework.data.repository.query.Param("excludeUserId") UUID excludeUserId);
}

