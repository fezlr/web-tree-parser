package com.fezlr.web_tree_parser.history.repository;

import com.fezlr.web_tree_parser.history.entity.ParseHistoryEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParseHistorySpecifications {

    public static Specification<ParseHistoryEntity> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<ParseHistoryEntity> sourceContains(String text) {
        String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.<String>get("source")), pattern, '\\');
    }

    public static Specification<ParseHistoryEntity> createdFrom(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), from);
    }

    public static Specification<ParseHistoryEntity> createdBefore(Instant to) {
        return (root, query, cb) -> cb.lessThan(root.<Instant>get("createdAt"), to);
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
