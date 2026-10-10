package com.fezlr.web_tree_parser.parsing.exporter.controller;

import com.fezlr.web_tree_parser.TestcontainersConfiguration;
import com.fezlr.web_tree_parser.auth.security.UserDetailsImpl;
import com.fezlr.web_tree_parser.core.lexer.Lexer;
import com.fezlr.web_tree_parser.core.node.Node;
import com.fezlr.web_tree_parser.core.parser.Parser;
import com.fezlr.web_tree_parser.core.stats.StatsAssemble;
import com.fezlr.web_tree_parser.history.mapper.ParseHistoryMapper;
import com.fezlr.web_tree_parser.history.service.ParseHistoryService;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.service.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestcontainersConfiguration.class)
class FileExportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ParseHistoryService parseHistoryService;

    @Autowired
    private ParseHistoryMapper parseHistoryMapper;

    @Test
    void shouldDownloadJson_whenFormatIsDefault() throws Exception {
        UserEntity owner = newUser("expctljson", Role.USER);
        Long id = save(owner, "{1, {a}}");

        mockMvc.perform(get("/results/" + id + "/export").with(loginAs(owner)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"parse-result-" + id + ".json\""))
                .andExpect(jsonPath("$.tree.type").value("set"))
                .andExpect(jsonPath("$.statistics.totalElements").value(2));
    }

    @Test
    void shouldDownloadXml_whenXmlFormatIsRequested() throws Exception {
        UserEntity owner = newUser("expctlxml", Role.USER);
        Long id = save(owner, "{1, {a}}");

        mockMvc.perform(get("/results/" + id + "/export").param("format", "xml").with(loginAs(owner)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"parse-result-" + id + ".xml\""))
                .andExpect(xpath("/parseResult/@id").string(String.valueOf(id)))
                .andExpect(xpath("/parseResult/tree/set/number").string("1"));
    }

    @Test
    void shouldResolveFormat_whenInputFormatIsProvidedInMixedCase() throws Exception {
        UserEntity owner = newUser("expctlcase", Role.USER);
        Long id = save(owner, "{1}");

        mockMvc.perform(get("/results/" + id + "/export").param("format", "XML").with(loginAs(owner)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
    }

    @Test
    void shouldReturnBadRequest_whenFormatIsUnsupported() throws Exception {
        UserEntity owner = newUser("expctlbad", Role.USER);
        Long id = save(owner, "{1}");

        mockMvc.perform(get("/results/" + id + "/export").param("format", "yaml").with(loginAs(owner)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFound_whenUserIsNotOwnerOfEntry() throws Exception {
        UserEntity owner = newUser("expctlowner", Role.USER);
        UserEntity stranger = newUser("expctlstranger", Role.USER);
        Long id = save(owner, "{1}");

        mockMvc.perform(get("/results/" + id + "/export").with(loginAs(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/results/" + id + "/export").param("format", "xml").with(loginAs(stranger)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFound_whenEntryDoesNotExist() throws Exception {
        UserEntity userEntity = newUser("expctlunknown", Role.USER);

        mockMvc.perform(get("/results/-1/export").with(loginAs(userEntity)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowDownload_whenRequestedByAdministrator() throws Exception {
        UserEntity owner = newUser("expctladminowner", Role.USER);
        UserEntity admin = newUser("expctladmin", Role.ADMIN);
        Long id = save(owner, "{1}");

        mockMvc.perform(get("/results/" + id + "/export").with(loginAs(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("expctladminowner"));
    }

    @Test
    void shouldRedirectToLogin_whenUserIsAnonymous() throws Exception {
        UserEntity userEntity = newUser("expctlanon", Role.USER);
        Long id = save(userEntity, "{1}");

        mockMvc.perform(get("/results/" + id + "/export"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private UserEntity newUser(String username, Role role) {
        return userService.create(username, "password123", role);
    }

    private RequestPostProcessor loginAs(UserEntity account) {
        return user(new UserDetailsImpl(account));
    }

    private Long save(UserEntity userEntity, String source) {
        Node tree = new Parser(new Lexer(source).tokenize()).parse();
        return parseHistoryService.save(parseHistoryMapper.toEntity(userEntity, source, new StatsAssemble().assemble(tree))).getId();
    }
}
