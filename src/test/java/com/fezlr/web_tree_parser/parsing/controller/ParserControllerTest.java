package com.fezlr.web_tree_parser.parsing.controller;

import com.fezlr.web_tree_parser.TestcontainersConfiguration;
import com.fezlr.web_tree_parser.auth.security.UserDetailsImpl;
import com.fezlr.web_tree_parser.history.service.ParseHistoryService;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ParserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ParseHistoryService parseHistoryService;

    @Test
    void shouldShowForm_whenAccessingRoot() throws Exception {
        UserEntity account = account("formviewer");

        mockMvc.perform(get("/").with(loginAs(account)))
                .andExpect(status().isOk())
                .andExpect(view().name("parser/index"))
                .andExpect(model().attributeExists("parseRequest"));
    }

    @Test
    void shouldParseTree_whenInputIsValid() throws Exception {
        UserEntity account = account("parseowner");

        mockMvc.perform(post("/parse").with(csrf()).with(loginAs(account))
                        .param("input", "{1, {a, \"x\"}}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/results/*"));
    }

    @Test
    void savesSuccessfulParseToHistory() throws Exception {
        UserEntity account = account("historysaver");
        long before = historySize(account);

        mockMvc.perform(post("/parse").with(csrf()).with(loginAs(account))
                        .param("input", "{savedset}"))
                .andExpect(status().is3xxRedirection());

        assertThat(historySize(account)).isEqualTo(before + 1);
    }

    @Test
    void showsResultPageAfterParsing() throws Exception {
        UserEntity account = account("resultviewer");

        mockMvc.perform(post("/parse").with(csrf()).with(loginAs(account))
                        .param("input", "{1, {\"a\"}}"))
                .andExpect(status().is3xxRedirection());

        var historyPage = parseHistoryService.findHistories(account, PageRequest.of(0, 1, Sort.by("createdAt", "id").descending()));
        var content = historyPage.getContent();

        assertThat(content.isEmpty()).isFalse();

        Long savedId = content.getFirst().id();

        mockMvc.perform(get("/results/" + savedId).with(loginAs(account)))
                .andExpect(status().isOk())
                .andExpect(view().name("parser/result"))
                .andExpect(model().attributeExists("entry", "result"));
    }

    @Test
    void shouldShowError_whenInputIsNotValid() throws Exception {
        UserEntity account = account("errorviewer");
        long before = historySize(account);

        mockMvc.perform(post("/parse").with(csrf()).with(loginAs(account))
                        .param("input", "{1,"))
                .andExpect(status().isOk())
                .andExpect(view().name("parser/index"))
                .andExpect(model().attributeHasFieldErrors("parseRequest", "input"));

        assertThat(historySize(account)).isEqualTo(before);
    }

    @Test
    void shouldShowError_whenInputIsBlank() throws Exception {
        UserEntity account = account("blankviewer");

        mockMvc.perform(post("/parse").with(csrf()).with(loginAs(account))
                        .param("input", "   "))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("parseRequest", "input"));
    }

    @Test
    void shouldShowError_whenInputIsTooLong() throws Exception {
        UserEntity account = account("longviewer");

        mockMvc.perform(post("/parse").with(csrf()).with(loginAs(account))
                        .param("input", "a".repeat(100_001)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("parseRequest", "input"));
    }

    @Test
    void shouldParseTree_whenInputIsAtMaxLength() throws Exception {
        UserEntity account = account("maxlengthviewer");
        String maxValidInput = "{" + "a,".repeat(49_998) + "a}";

        mockMvc.perform(post("/parse").with(csrf()).with(loginAs(account))
                        .param("input", maxValidInput))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/results/*"));
    }

    @Test
    void shouldRedirectToLogin_whenUserIsAnonymous() throws Exception {
        mockMvc.perform(post("/parse").with(csrf())
                        .param("input", "{1}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void shouldRedirectToLogin_whenUserLogsOut() throws Exception {
        UserEntity account = account("logoutviewer");

        mockMvc.perform(post("/logout").with(csrf()).with(loginAs(account)))
                .andExpect(redirectedUrl("/login?logout"));
    }

    private UserEntity account(String username) {
        return userService.findByUsername(username)
                .orElseGet(() -> userService.create(username, "password123", Role.USER));
    }

    private RequestPostProcessor loginAs(UserEntity account) {
        return user(new UserDetailsImpl(account));
    }

    private long historySize(UserEntity account) {
        return parseHistoryService.findHistories(account, PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .getTotalElements();
    }
}
