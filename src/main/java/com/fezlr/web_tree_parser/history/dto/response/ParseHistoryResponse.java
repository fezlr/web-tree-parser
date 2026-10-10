package com.fezlr.web_tree_parser.history.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ParseHistoryResponse(

        Long id,

        String author,

        String source,

        int totalElements,

        int setCount,

        int subsets,

        int maxDepth,

        int numberCount,

        int identifierCount,

        int stringCount,

        LocalDateTime createdAt
) {
}
