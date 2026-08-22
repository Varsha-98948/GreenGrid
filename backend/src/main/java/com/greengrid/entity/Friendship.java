package com.greengrid.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "friendships", uniqueConstraints = {
        @UniqueConstraint(name = "uk_friendship_pair", columnNames = {"user_id1", "user_id2"})
})
@Getter
@Setter
@NoArgsConstructor
public class Friendship extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id1", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id2", nullable = false)
    private User user2;
}
