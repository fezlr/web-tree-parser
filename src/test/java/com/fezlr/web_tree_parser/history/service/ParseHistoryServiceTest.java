package com.fezlr.web_tree_parser.history.service;

import com.fezlr.web_tree_parser.TestcontainersConfiguration;
import com.fezlr.web_tree_parser.core.lexer.Lexer;
import com.fezlr.web_tree_parser.core.node.Node;
import com.fezlr.web_tree_parser.core.parser.Parser;
import com.fezlr.web_tree_parser.core.stats.Stats;
import com.fezlr.web_tree_parser.core.stats.StatsAssemble;
import com.fezlr.web_tree_parser.history.dto.request.ParserHistoryFilterRequest;
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
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ParseHistoryServiceTest {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");
    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20, NEWEST_FIRST);

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

    @Test
    void shouldFindHistoriesIgnoringCase_whenSubstringIsProvided() {
        UserEntity user = userService.create("searchcase", "password123", Role.USER);
        save(user, "{Alpha, beta}");
        save(user, "{gamma}");

        assertThat(sources(user, filter("ALPHA", null, null))).containsExactly("{Alpha, beta}");
        assertThat(sources(user, filter("mm", null, null))).containsExactly("{gamma}");
    }

    @Test
    void shouldTreatSpecialCharactersLiterally_whenSearchingWithWildcards() {
        UserEntity user = userService.create("searchwild", "password123", Role.USER);
        save(user, "{\"a_b\"}");
        save(user, "{\"axb\"}");
        save(user, "{\"100%\"}");
        save(user, "{\"1000\"}");

        assertThat(sources(user, filter("a_b", null, null))).containsExactly("{\"a_b\"}");
        assertThat(sources(user, filter("%", null, null))).containsExactly("{\"100%\"}");
    }

    @Test
    void shouldReturnAllEntries_whenSearchTextIsBlankOrNull() {
        UserEntity user = userService.create("searchblank", "password123", Role.USER);
        save(user, "{1}");
        save(user, "{2}");

        assertThat(sources(user, filter("   ", null, null))).hasSize(2);
        assertThat(sources(user, filter(null, null, null))).hasSize(2);
    }

    @Test
    void shouldFilterHistoriesInclusively_whenDateRangeIsProvided() {
        UserEntity user = userService.create("searchdates", "password123", Role.USER);
        save(user, "{1}");
        LocalDate today = LocalDate.now();

        assertThat(sources(user, filter(null, today, today))).hasSize(1);
        assertThat(sources(user, filter(null, today.minusDays(3), today.plusDays(3)))).hasSize(1);
        assertThat(sources(user, filter(null, today.plusDays(1), null))).isEmpty();
        assertThat(sources(user, filter(null, null, today.minusDays(1)))).isEmpty();
    }

    @Test
    void shouldCombineFilters_whenBothTextAndDateAreProvided() {
        UserEntity user = userService.create("searchcombo", "password123", Role.USER);
        save(user, "{needle}");
        save(user, "{haystack}");
        LocalDate today = LocalDate.now();

        assertThat(sources(user, filter("needle", today, today))).containsExactly("{needle}");
        assertThat(sources(user, filter("needle", today.plusDays(1), null))).isEmpty();
    }

    @Test
    void shouldNeverLeakOtherUsersEntries_whenRegularUserSearches() {
        UserEntity first = userService.create("searchprivate1", "password123", Role.USER);
        UserEntity second = userService.create("searchprivate2", "password123", Role.USER);
        save(first, "{shared}");
        save(second, "{shared}");

        Page<ParseHistoryResponse> page = parseHistoryService.findHistories(first, filter("shared", null, null), FIRST_PAGE);

        assertThat(page.getContent()).extracting(ParseHistoryResponse::author).containsExactly("searchprivate1");
    }

    @Test
    void shouldSpanAllUsersEntries_whenAdministratorSearches() {
        UserEntity first = userService.create("searchspan1", "password123", Role.USER);
        UserEntity second = userService.create("searchspan2", "password123", Role.USER);
        UserEntity admin = userService.create("searchspanadmin", "password123", Role.ADMIN);
        save(first, "{spanmarker}");
        save(second, "{spanmarker}");

        Page<ParseHistoryResponse> page = parseHistoryService.findHistories(admin, filter("spanmarker", null, null), FIRST_PAGE);

        assertThat(page.getContent()).extracting(ParseHistoryResponse::author)
                .containsExactlyInAnyOrder("searchspan1", "searchspan2");
    }

    @Test
    void shouldPaginateFilteredResults_whenPageRequestIsSpecified() {
        UserEntity user = userService.create("searchpages", "password123", Role.USER);
        for (int i = 0; i < 5; i++) {
            save(user, "{pagemarker" + i + "}");
        }
        save(user, "{other}");

        Page<ParseHistoryResponse> page = parseHistoryService.findHistories(
                user, filter("pagemarker", null, null), PageRequest.of(1, 2, NEWEST_FIRST));

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }

    private Long save(UserEntity userEntity, String source) {
        Node tree = new Parser(new Lexer(source).tokenize()).parse();
        Stats stats = new StatsAssemble().assemble(tree);
        ParseHistoryEntity entity = parseHistoryMapper.toEntity(userEntity, source, stats);

        ParseHistoryEntity savedEntity = parseHistoryRepository.save(entity);

        return savedEntity.getId();
    }

    private ParserHistoryFilterRequest filter(String text, LocalDate from, LocalDate to) {
        ParserHistoryFilterRequest filter = new ParserHistoryFilterRequest();
        filter.setQuery(text);
        filter.setFrom(from);
        filter.setTo(to);
        return filter;
    }

    private List<String> sources(UserEntity viewer, ParserHistoryFilterRequest filter) {
        return parseHistoryService.findHistories(viewer, filter, FIRST_PAGE).getContent().stream()
                .map(ParseHistoryResponse::source)
                .toList();
    }
}

