package com.fezlr.web_tree_parser.history.repository;

import com.fezlr.web_tree_parser.history.entity.ParseHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParseHistoryRepository extends JpaRepository<ParseHistoryEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "user")
    Page<ParseHistoryEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<ParseHistoryEntity> findByUserId(Long userId, Pageable pageable);
}
