package com.fezlr.web_tree_parser.auth.security;

import com.fezlr.web_tree_parser.TestcontainersConfiguration;
import com.fezlr.web_tree_parser.admin.service.AdminUserService;
import com.fezlr.web_tree_parser.core.lexer.Lexer;
import com.fezlr.web_tree_parser.core.node.Node;
import com.fezlr.web_tree_parser.core.parser.Parser;
import com.fezlr.web_tree_parser.core.stats.StatsAssemble;
import com.fezlr.web_tree_parser.history.mapper.ParseHistoryMapper;
import com.fezlr.web_tree_parser.history.service.ParseHistoryService;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.repository.UserRepository;
import com.fezlr.web_tree_parser.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CurrentUserFilterTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private ParseHistoryService parseHistoryService;

    @Autowired
    private ParseHistoryMapper parseHistoryMapper;

    @Test
    void shouldKeepSession_whenUserIsUnchanged() throws Exception {
        newUser("filterstable", Role.USER);
        MockHttpSession session = login("filterstable");

        mockMvc.perform(get("/history").session(session)).andExpect(status().isOk());
        mockMvc.perform(get("/history").session(session)).andExpect(status().isOk());

        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    void shouldInvalidateSession_whenUserIsBlocked() throws Exception {
        UserEntity admin = newUser("filterblockadmin", Role.ADMIN);
        UserEntity member = newUser("filterblockmember", Role.USER);
        MockHttpSession session = login("filterblockmember");
        mockMvc.perform(get("/history").session(session)).andExpect(status().isOk());

        adminUserService.setEnabled(member.getId(), false, admin);

        mockMvc.perform(get("/history").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?blocked"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void shouldDenyAdminAreaImmediately_whenAdminIsDemoted() throws Exception {
        UserEntity first = newUser("filterdemoteone", Role.ADMIN);
        UserEntity second = newUser("filterdemotetwo", Role.ADMIN);
        MockHttpSession session = login("filterdemoteone");
        mockMvc.perform(get("/admin/security-check").session(session)).andExpect(status().isNotFound());

        adminUserService.changeRole(first.getId(), Role.USER, second);

        mockMvc.perform(get("/admin/security-check").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyAccessToOthersHistoryImmediately_whenAdminIsDemoted() throws Exception {
        UserEntity first = newUser("filterhistoryone", Role.ADMIN);
        UserEntity second = newUser("filterhistorytwo", Role.ADMIN);
        UserEntity owner = newUser("filterhistoryowner", Role.USER);
        Long id = save(owner, "{1}");
        MockHttpSession session = login("filterhistoryone");
        mockMvc.perform(get("/results/" + id).session(session)).andExpect(status().isOk());

        adminUserService.changeRole(first.getId(), Role.USER, second);

        mockMvc.perform(get("/results/" + id).session(session)).andExpect(status().isNotFound());
    }

    @Test
    void shouldGrantAdminAreaImmediately_whenUserIsPromoted() throws Exception {
        UserEntity admin = newUser("filterpromoteadmin", Role.ADMIN);
        UserEntity member = newUser("filterpromotemember", Role.USER);
        MockHttpSession session = login("filterpromotemember");
        mockMvc.perform(get("/admin/security-check").session(session)).andExpect(status().isForbidden());

        adminUserService.changeRole(member.getId(), Role.ADMIN, admin);

        mockMvc.perform(get("/admin/security-check").session(session)).andExpect(status().isNotFound());
    }

    @Test
    void shouldInvalidateSessionImmediately_whenUserIsDisabled() throws Exception {
        UserEntity member = newUser("filterdeletedmember", Role.USER);
        MockHttpSession session = login("filterdeletedmember");

        mockMvc.perform(get("/history").session(session)).andExpect(status().isOk());

        member.setEnabled(false);
        userRepository.save(member);

        mockMvc.perform(get("/history").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?blocked"));
        assertThat(session.isInvalid()).isTrue();
    }

    private UserEntity newUser(String username, Role role) {
        return userService.create(username, "password123", role);
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult result = mockMvc.perform(formLogin().user(username).password("password123"))
                .andExpect(authenticated())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Long save(UserEntity author, String source) {
        Node tree = new Parser(new Lexer(source).tokenize()).parse();

        return parseHistoryService
                .save(parseHistoryMapper
                .toEntity(author, source, new StatsAssemble().assemble(tree)))
                .getId();
    }
}
