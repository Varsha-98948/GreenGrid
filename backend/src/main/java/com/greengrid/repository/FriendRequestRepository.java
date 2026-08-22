package com.greengrid.repository;

import com.greengrid.entity.FriendRequest;
import com.greengrid.entity.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.addressee.id = :userId AND fr.status = :status ORDER BY fr.createdAt DESC")
    List<FriendRequest> findByAddresseeIdAndStatus(@Param("userId") UUID userId, @Param("status") FriendRequestStatus status);

    @Query("SELECT fr FROM FriendRequest fr WHERE fr.requester.id = :userId AND fr.status = :status ORDER BY fr.createdAt DESC")
    List<FriendRequest> findByRequesterIdAndStatus(@Param("userId") UUID userId, @Param("status") FriendRequestStatus status);

    @Query("SELECT fr FROM FriendRequest fr WHERE " +
           "((fr.requester.id = :u1 AND fr.addressee.id = :u2) OR (fr.requester.id = :u2 AND fr.addressee.id = :u1)) " +
           "AND fr.status = 'PENDING'")
    Optional<FriendRequest> findPendingRequestBetween(@Param("u1") UUID u1, @Param("u2") UUID u2);

    @Query("SELECT fr FROM FriendRequest fr WHERE " +
           "(fr.requester.id = :u1 AND fr.addressee.id = :u2) OR (fr.requester.id = :u2 AND fr.addressee.id = :u1)")
    List<FriendRequest> findAllRequestsBetween(@Param("u1") UUID u1, @Param("u2") UUID u2);
}
