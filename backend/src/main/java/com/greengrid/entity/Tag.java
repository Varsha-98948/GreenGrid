package com.greengrid.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A topic/tag (e.g. "Dynamic Programming", "Two Pointers"), normalized and
 * scoped per-user so that tag analytics (topic breakdown) can be computed
 * with a simple group-by rather than string matching across free-text.
 */
@Entity
@Table(name = "tags", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tag_user_name", columnNames = {"user_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
public class Tag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    public Tag(User user, String name) {
        this.user = user;
        this.name = name;
    }
}
