package com.fezlr.web_tree_parser.history.service;

import com.fezlr.web_tree_parser.history.dto.request.ParserHistoryFilterRequest;
import com.fezlr.web_tree_parser.history.dto.response.ParseHistoryResponse;
import com.fezlr.web_tree_parser.history.entity.ParseHistoryEntity;
import com.fezlr.web_tree_parser.history.exception.ParseHistoryNotFoundException;
import com.fezlr.web_tree_parser.history.mapper.ParseHistoryMapper;
import com.fezlr.web_tree_parser.history.repository.ParseHistoryRepository;
import com.fezlr.web_tree_parser.history.repository.ParseHistorySpecifications;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RequiredArgsConstructor
@Service
public class ParseHistoryService {
    private final ParseHistoryRepository parseHistoryRepository;
    private final ParseHistoryMapper parseHistoryMapper;

    @Transactional(readOnly = true)
    public ParseHistoryResponse findAccessibleHistory(Long id, UserEntity userEntity) {
        ParseHistoryEntity entity = parseHistoryRepository
                .findById(id)
                .orElseThrow(() -> new ParseHistoryNotFoundException(id));

        if (!isAdmin(userEntity) && !entity.getUser().getId().equals(userEntity.getId())) {
            throw new ParseHistoryNotFoundException(id);
        }

        return parseHistoryMapper.toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<ParseHistoryResponse> findHistories(UserEntity userEntity, Pageable pageable) {
        return findHistories(userEntity, new ParserHistoryFilterRequest(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<ParseHistoryResponse> findHistories(UserEntity userEntity, ParserHistoryFilterRequest filter, Pageable pageable) {
        Specification<ParseHistoryEntity> spec = Specification.unrestricted();

        if (!isAdmin(userEntity)) {
            spec = spec.and(ParseHistorySpecifications.ownedBy(userEntity.getId()));
        }

        if (filter.getQuery() != null && !filter.getQuery().isBlank()) {
            spec = spec.and(ParseHistorySpecifications.sourceContains(filter.getQuery().trim()));
        }

        if (filter.getFrom() != null) {
            spec = spec.and(ParseHistorySpecifications.createdFrom(startOfDay(filter.getFrom())));
        }

        if (filter.getTo() != null) {
            spec = spec.and(ParseHistorySpecifications.createdBefore(startOfDay(filter.getTo().plusDays(1))));
        }

        return parseHistoryRepository.findAll(spec, pageable).map(parseHistoryMapper::toResponse);
    }

    @Transactional
    public ParseHistoryEntity save(ParseHistoryEntity entity) {
        return parseHistoryRepository.save(entity);
    }

    private boolean isAdmin(UserEntity userEntity) {
        return userEntity.getRole() == Role.ADMIN;
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
