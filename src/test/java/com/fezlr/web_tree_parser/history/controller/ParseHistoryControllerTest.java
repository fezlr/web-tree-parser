package com.fezlr.web_tree_parser.history.controller;

import com.fezlr.web_tree_parser.TestcontainersConfiguration;
import com.fezlr.web_tree_parser.auth.security.UserDetailsImpl;
import com.fezlr.web_tree_parser.history.mapper.ParseHistoryMapper;
import com.fezlr.web_tree_parser.history.service.ParseHistoryService;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class ParseHistoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private ParseHistoryService parseHistoryService;

    @Autowired
    private ParseHistoryMapper parseHistoryMapper;

    @Test
    void shouldOpenResult_whenUserIsOwner() throws Exception {
        UserEntity owner = account("accessowner", Role.USER);
        String location = parseAs(owner, "{1}");

        mockMvc.perform(get(location).with(loginAs(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnNotFoundException_whenUserIsNotOwner() throws Exception {
        UserEntity owner = account("accesshost", Role.USER);
        UserEntity stranger = account("accessguest", Role.USER);
        String location = parseAs(owner, "{1}");

        mockMvc.perform(get(location).with(loginAs(stranger)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldOpenAnyResult_whenUserIsAdministrator() throws Exception {
        UserEntity owner = account("accessmember", Role.USER);
        UserEntity admin = account("accessadmin", Role.ADMIN);
        String location = parseAs(owner, "{1}");

        mockMvc.perform(get(location).with(loginAs(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRedirectToLogin_whenUserIsAnonymous() throws Exception {
        UserEntity owner = account("accessprivate", Role.USER);
        String location = parseAs(owner, "{1}");

        mockMvc.perform(get(location))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void shouldShowOnlyOwnEntries_whenUserRequestsHistoryList() throws Exception {
        UserEntity alice = account("listalice", Role.USER);
        UserEntity bob = account("listbob", Role.USER);

        parseAs(alice, "{aliceonly}");
        parseAs(bob, "{bobonly}");

        String body = historyPageAs(alice);

        assertThat(body).contains("aliceonly").doesNotContain("bobonly");
    }

    @Test
    void shouldShowEveryonesHistoryWithAuthors_whenUserIsAdministrator() throws Exception {
        UserEntity alice = account("adminviewalice", Role.USER);
        UserEntity bob = account("adminviewbob", Role.USER);
        UserEntity admin = account("adminviewer", Role.ADMIN);

        parseAs(alice, "{firstmarker}");
        parseAs(bob, "{secondmarker}");

        String body = historyPageAs(admin);

        assertThat(body).contains("firstmarker", "secondmarker", "adminviewalice", "adminviewbob");
    }

    @Test
    void shouldFilterListByText_whenQueryIsProvided() throws Exception {
        UserEntity account = account("ctlsearch", Role.USER);
        parseAs(account, "{needleset}");
        parseAs(account, "{haystack}");

        String body = mockMvc.perform(get("/history?query=needle").with(loginAs(account)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("{needleset}").doesNotContain("haystack");
    }

    @Test
    void shouldRejectInvertedDateRange_whenFromIsAfterTo() throws Exception {
        UserEntity account = account("ctlrange", Role.USER);

        mockMvc.perform(get("/history?from=2026-10-10&to=2026-10-01").with(loginAs(account)))
                .andExpect(status().isOk())
                .andExpect(view().name("history/list"))
                .andExpect(model().attributeHasFieldErrors("filter", "to"));
    }

    @Test
    void shouldRejectTooLongSearchText_whenQueryReachLimit() throws Exception {
        UserEntity account = account("ctllong", Role.USER);

        mockMvc.perform(get("/history").param("query", "a".repeat(101)).with(loginAs(account)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("filter", "query"));
    }

    @Test
    void shouldTreatNegativePageAsFirstPage_whenPageParameterIsInvalid() throws Exception {
        UserEntity account = account("ctlnegative", Role.USER);
        parseAs(account, "{negativemarker}");

        String body = mockMvc.perform(get("/history?page=-5").with(loginAs(account)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("negativemarker");
    }

    private UserEntity account(String username, Role role) {
        return userService.create(username, "password123", role);
    }

    private RequestPostProcessor loginAs(UserEntity account) {
        return user(new UserDetailsImpl(account));
    }

    private String parseAs(UserEntity account, String input) throws Exception {
        return mockMvc.perform(post("/parse").with(csrf()).with(loginAs(account)).param("input", input))
                .andReturn().getResponse().getRedirectedUrl();
    }

    private String historyPageAs(UserEntity account) throws Exception {
        return mockMvc.perform(get("/history").with(loginAs(account)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
