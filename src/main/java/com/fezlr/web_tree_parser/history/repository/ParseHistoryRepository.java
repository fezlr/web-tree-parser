package com.fezlr.web_tree_parser.history.repository;

import com.fezlr.web_tree_parser.history.entity.ParseHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface ParseHistoryRepository extends JpaRepository<ParseHistoryEntity, Long>, JpaSpecificationExecutor<ParseHistoryEntity> {

    @Override
    @EntityGraph(attributePaths = "user")
    Optional<ParseHistoryEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<ParseHistoryEntity> findAll(Specification<ParseHistoryEntity> spec, Pageable pageable);

}
