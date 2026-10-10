package com.fezlr.web_tree_parser.history.service;

import com.fezlr.web_tree_parser.TestcontainersConfiguration;
import com.fezlr.web_tree_parser.core.lexer.Lexer;
import com.fezlr.web_tree_parser.core.node.Node;
import com.fezlr.web_tree_parser.core.parser.Parser;
import com.fezlr.web_tree_parser.core.stats.Stats;
import com.fezlr.web_tree_parser.core.stats.StatsAssemble;
import com.fezlr.web_tree_parser.history.dto.response.ParseHistoryResponse;
import com.fezlr.web_tree_parser.history.entity.ParseHistoryEntity;
import com.fezlr.web_tree_parser.history.exception.ParseHistoryNotFoundException;
import com.fezlr.web_tree_parser.history.mapper.ParseHistoryMapper;
import com.fezlr.web_tree_parser.history.repository.ParseHistoryRepository;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ParseHistoryServiceTest {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    @Autowired
    private ParseHistoryService parseHistoryService;

    @Autowired
    private ParseHistoryRepository parseHistoryRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ParseHistoryMapper parseHistoryMapper;


    @Test
    void shouldSaveEntryWithStatistics_whenInputIsValid() {
        UserEntity userEntity = userService.create("svcauthor", "password123", Role.USER);

        Long id = save(userEntity, "{1, {a}}");
        ParseHistoryResponse item = parseHistoryService.findAccessibleHistory(id, userEntity);

        assertThat(item.author()).isEqualTo("svcauthor");
        assertThat(item.source()).isEqualTo("{1, {a}}");
        assertThat(item.totalElements()).isEqualTo(2);
        assertThat(item.maxDepth()).isEqualTo(2);
    }

    @Test
    void shouldHideEntry_whenAccessedByOtherUser() {
        UserEntity owner = userService.create("svcowner", "password123", Role.USER);
        UserEntity stranger = userService.create("svcstranger", "password123", Role.USER);
        Long id = save(owner, "{1}");

        assertThatThrownBy(() -> parseHistoryService.findAccessibleHistory(id, stranger))
                .isInstanceOf(ParseHistoryNotFoundException.class);
    }

    @Test
    void shouldLetAdministratorReadAnyEntry_whenRequested() {
        UserEntity owner = userService.create("svcmember", "password123", Role.USER);
        UserEntity admin = userService.create("svcadmin", "password123", Role.ADMIN);
        Long id = save(owner, "{1}");

        assertThat(parseHistoryService.findAccessibleHistory(id, admin).author()).isEqualTo("svcmember");
    }

    @Test
    void shouldThrowNotFoundException_whenEntryDoesNotExist() {
        UserEntity userEntity = userService.create("svcviewer", "password123", Role.USER);

        assertThatThrownBy(() -> parseHistoryService.findAccessibleHistory(-1L, userEntity))
                .isInstanceOf(ParseHistoryNotFoundException.class);
    }

    @Test
    void shouldFindHistoriesOnlyOwnEntries_whenUserIsRegular() {
        UserEntity lister = userService.create("svclister", "password123", Role.USER);
        UserEntity other = userService.create("svcother", "password123", Role.USER);
        save(lister, "{1}");
        save(lister, "{2}");
        save(lister, "{3}");
        save(other, "{4}");

        Page<ParseHistoryResponse> page = parseHistoryService.findHistories(lister, PageRequest.of(0, 2, NEWEST_FIRST));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(ParseHistoryResponse::author).containsOnly("svclister");
    }

    private Long save(UserEntity userEntity, String source) {
        Node tree = new Parser(new Lexer(source).tokenize()).parse();
        Stats stats = new StatsAssemble().assemble(tree);
        ParseHistoryEntity entity = parseHistoryMapper.toEntity(userEntity, source, stats);

        ParseHistoryEntity savedEntity = parseHistoryRepository.save(entity);

        return savedEntity.getId();
    }

}

