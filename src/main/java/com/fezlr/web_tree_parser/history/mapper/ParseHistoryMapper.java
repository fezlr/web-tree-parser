package com.fezlr.web_tree_parser.history.mapper;

import com.fezlr.web_tree_parser.core.stats.Stats;
import com.fezlr.web_tree_parser.history.dto.response.ParseHistoryResponse;
import com.fezlr.web_tree_parser.history.entity.ParseHistoryEntity;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class ParseHistoryMapper {
    public ParseHistoryEntity toEntity(UserEntity userEntity, String source, Stats stats) {
        return ParseHistoryEntity.builder()
                .user(userEntity)
                .source(source)
                .totalElements(stats.totalElements())
                .setCount(stats.setCount())
                .subsets(stats.subsets())
                .maxDepth(stats.maxDepth())
                .numberCount(stats.numberCount())
                .identifierCount(stats.identifierCount())
                .stringCount(stats.stringCount())
                .build();
    }

    public ParseHistoryResponse toResponse(ParseHistoryEntity entity) {
        return ParseHistoryResponse.builder()
                .id(entity.getId())
                .author(entity.getUser().getUsername())
                .source(entity.getSource())
                .totalElements(entity.getTotalElements())
                .setCount(entity.getSetCount())
                .subsets(entity.getSubsets())
                .maxDepth(entity.getMaxDepth())
                .numberCount(entity.getNumberCount())
                .identifierCount(entity.getIdentifierCount())
                .stringCount(entity.getStringCount())
                .build();
    }
}
