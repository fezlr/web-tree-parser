package com.fezlr.web_tree_parser.user.repository;

import com.fezlr.web_tree_parser.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
}
