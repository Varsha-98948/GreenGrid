package com.greengrid.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "friend_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_friend_request_pair", columnNames = {"requester_id", "addressee_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class FriendRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FriendRequestStatus status = FriendRequestStatus.PENDING;
}
