package com.fezlr.web_tree_parser.parsing.importer.controller;

import com.fezlr.web_tree_parser.TestcontainersConfiguration;
import com.fezlr.web_tree_parser.auth.security.UserDetailsImpl;
import com.fezlr.web_tree_parser.history.dto.response.ParseHistoryResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestcontainersConfiguration.class)
class FileImportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ParseHistoryService historyService;

    @Test
    void shouldImportFileAndSaveInHistory_whenFileIsValid() throws Exception {
        UserEntity account = newUser("importowner");

        mockMvc.perform(multipart("/parse/file")
                        .file(file("set.txt", "{1, {a, \"x\"}}"))
                        .with(csrf()).with(loginAs(account)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/results/*"));

        assertThat(history(account)).extracting(ParseHistoryResponse::source).containsExactly("{1, {a, \"x\"}}");
    }

    @Test
    void shouldOpenImportedResult_whenRequestedAfterUpload() throws Exception {
        UserEntity account = newUser("importviewer");

        String location = mockMvc.perform(multipart("/parse/file")
                        .file(file("set.txt", "{1, {a}}"))
                        .with(csrf()).with(loginAs(account)))
                .andReturn().getResponse().getRedirectedUrl();

        assertThat(location).isNotNull();

        mockMvc.perform(get(location).with(loginAs(account)))
                .andExpect(status().isOk())
                .andExpect(view().name("parser/result"));
    }

    @Test
    void shouldCleanContent_whenFileContainsBomAndWindowsLineBreaks() throws Exception {
        UserEntity account = newUser("importbom");

        mockMvc.perform(multipart("/parse/file")
                        .file(file("set.txt", "\uFEFF{\r\n  1,\r\n  2\r\n}\r\n"))
                        .with(csrf()).with(loginAs(account)))
                .andExpect(status().is3xxRedirection());

        assertThat(history(account)).extracting(ParseHistoryResponse::source).containsExactly("{\r\n  1,\r\n  2\r\n}\r\n");
    }

    @Test
    void shouldShowParseErrorAndNotSave_whenSetIsInvalid() throws Exception {
        UserEntity account = newUser("importbroken");

        mockMvc.perform(multipart("/parse/file")
                        .file(file("set.txt", "{1,"))
                        .with(csrf()).with(loginAs(account)))
                .andExpect(status().isOk())
                .andExpect(view().name("parser/index"))
                .andExpect(model().attributeExists("fileError"));

        assertThat(history(account)).isEmpty();
    }

    @Test
    void shouldRejectUpload_whenExtensionIsUnsupported() throws Exception {
        UserEntity account = newUser("importwrongext");

        mockMvc.perform(multipart("/parse/file")
                        .file(file("set.csv", "{1}"))
                        .with(csrf()).with(loginAs(account)))
                .andExpect(status().isOk())
                .andExpect(view().name("parser/index"))
                .andExpect(model().attributeExists("fileError"));

        assertThat(history(account)).isEmpty();
    }

    @Test
    void shouldRejectUpload_whenNoFileIsSelected() throws Exception {
        UserEntity account = newUser("importnofile");
        MockMultipartFile empty = new MockMultipartFile("file", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/parse/file")
                        .file(empty)
                        .with(csrf()).with(loginAs(account)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("fileError"));

        assertThat(history(account)).isEmpty();
    }

    @Test
    void shouldRedirectToLogin_whenUserIsAnonymous() throws Exception {
        mockMvc.perform(multipart("/parse/file")
                        .file(file("set.txt", "{1}"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private UserEntity newUser(String username) {
        return userService.create(username, "password123", Role.USER);
    }

    private RequestPostProcessor loginAs(UserEntity account) {
        return user(new UserDetailsImpl(account));
    }

    private MockMultipartFile file(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain", content.getBytes(StandardCharsets.UTF_8));
    }

    private List<ParseHistoryResponse> history(UserEntity userEntity) {
        return historyService
                .findHistories(userEntity, PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .getContent();
    }
}
