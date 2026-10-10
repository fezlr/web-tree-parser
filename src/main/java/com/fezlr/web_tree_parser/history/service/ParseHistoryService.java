package com.fezlr.web_tree_parser.history.service;

import com.fezlr.web_tree_parser.history.dto.response.ParseHistoryResponse;
import com.fezlr.web_tree_parser.history.entity.ParseHistoryEntity;
import com.fezlr.web_tree_parser.history.exception.ParseHistoryNotFoundException;
import com.fezlr.web_tree_parser.history.mapper.ParseHistoryMapper;
import com.fezlr.web_tree_parser.history.repository.ParseHistoryRepository;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
        Page<ParseHistoryEntity> page = isAdmin(userEntity)
                ? parseHistoryRepository.findAll(pageable)
                : parseHistoryRepository.findByUserId(userEntity.getId(), pageable);

        return page.map(parseHistoryMapper::toResponse);
    }

    @Transactional
    public ParseHistoryEntity save(ParseHistoryEntity entity) {
        return parseHistoryRepository.save(entity);
    }

    private boolean isAdmin(UserEntity userEntity) {
        return userEntity.getRole() == Role.ADMIN;
    }
}
