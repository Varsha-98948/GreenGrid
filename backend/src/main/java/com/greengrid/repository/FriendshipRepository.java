package com.greengrid.repository;

import com.greengrid.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("SELECT f FROM Friendship f WHERE f.user1.id = :userId OR f.user2.id = :userId ORDER BY f.createdAt DESC")
    List<Friendship> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f WHERE " +
           "(f.user1.id = :u1 AND f.user2.id = :u2) OR (f.user1.id = :u2 AND f.user2.id = :u1)")
    boolean areFriends(@Param("u1") UUID u1, @Param("u2") UUID u2);

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.user1.id = :u1 AND f.user2.id = :u2) OR (f.user1.id = :u2 AND f.user2.id = :u1)")
    Optional<Friendship> findFriendshipBetween(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
