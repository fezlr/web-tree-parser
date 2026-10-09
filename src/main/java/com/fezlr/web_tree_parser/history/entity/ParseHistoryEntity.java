package com.fezlr.web_tree_parser.history.entity;

import com.fezlr.web_tree_parser.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "parse_history")
public class ParseHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String source;

    @Column(nullable = false, updatable = false)
    private int totalElements;

    @Column(nullable = false, updatable = false)
    private int setCount;

    @Column(nullable = false, updatable = false)
    private int subsets;

    @Column(nullable = false, updatable = false)
    private int maxDepth;

    @Column(nullable = false, updatable = false)
    private int numberCount;

    @Column(nullable = false, updatable = false)
    private int identifierCount;

    @Column(nullable = false, updatable = false)
    private int stringCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
