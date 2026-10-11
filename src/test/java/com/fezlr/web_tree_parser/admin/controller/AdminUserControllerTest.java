package com.fezlr.web_tree_parser.admin.controller;

import com.fezlr.web_tree_parser.TestcontainersConfiguration;
import com.fezlr.web_tree_parser.auth.security.UserDetailsImpl;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminUserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    void shouldListUsers_whenUserIsAdministrator() throws Exception {
        UserEntity admin = newUser("panellistadmin", Role.ADMIN);
        newUser("panellistmember", Role.USER);

        String body = mockMvc.perform(get("/admin/users").with(loginAs(admin)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("panellistadmin", "panellistmember");
    }

    @Test
    void shouldDenyAccessAndReturnForbidden_whenUserIsRegular() throws Exception {
        UserEntity member = newUser("panelforbidden", Role.USER);

        mockMvc.perform(get("/admin/users").with(loginAs(member)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRedirectToLogin_whenUserIsAnonymous() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void shouldChangeUserRole_whenFormIsSubmittedByAdmin() throws Exception {
        UserEntity admin = newUser("panelroleadmin", Role.ADMIN);
        UserEntity member = newUser("panelrolemember", Role.USER);

        mockMvc.perform(post("/admin/users/" + member.getId() + "/role")
                        .param("role", "ADMIN")
                        .param("page", "2")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?page=2"))
                .andExpect(flash().attributeExists("message"));

        assertThat(reload("panelrolemember").getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldBlockUserAndPreventLogin_whenBlockFormIsSubmittedByAdmin() throws Exception {
        UserEntity admin = newUser("panelblockadmin", Role.ADMIN);
        UserEntity member = newUser("panelblockmember", Role.USER);
        mockMvc.perform(formLogin().user("panelblockmember").password("password123"))
                .andExpect(authenticated());

        mockMvc.perform(post("/admin/users/" + member.getId() + "/enabled")
                        .param("enabled", "false")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(redirectedUrl("/admin/users?page=0"))
                .andExpect(flash().attributeExists("message"));

        assertThat(reload("panelblockmember").isEnabled()).isFalse();
        mockMvc.perform(formLogin().user("panelblockmember").password("password123"))
                .andExpect(unauthenticated());
    }

    @Test
    void shouldUnblockUser_whenUnblockFormIsSubmittedByAdmin() throws Exception {
        UserEntity admin = newUser("panelunblockadmin", Role.ADMIN);
        UserEntity member = newUser("panelunblockmember", Role.USER);

        mockMvc.perform(post("/admin/users/" + member.getId() + "/enabled")
                .param("enabled", "false")
                .with(csrf()).with(loginAs(admin)));
        mockMvc.perform(post("/admin/users/" + member.getId() + "/enabled")
                        .param("enabled", "true")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(status().is3xxRedirection());

        assertThat(reload("panelunblockmember").isEnabled()).isTrue();
        mockMvc.perform(formLogin().user("panelunblockmember").password("password123"))
                .andExpect(authenticated());
    }

    @Test
    void shouldThrowExceptionAndRedirect_whenAdminAttemptsToBlockOwnAccount() throws Exception {
        UserEntity admin = newUser("panelselfblock", Role.ADMIN);

        mockMvc.perform(post("/admin/users/" + admin.getId() + "/enabled")
                        .param("enabled", "false")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "You cannot block or unblock your own account"));

        assertThat(reload("panelselfblock").isEnabled()).isTrue();
    }

    @Test
    void shouldThrowExceptionAndRedirect_whenAdminAttemptsToChangeOwnRole() throws Exception {
        UserEntity admin = newUser("panelselfrole", Role.ADMIN);

        mockMvc.perform(post("/admin/users/" + admin.getId() + "/role")
                        .param("role", "USER")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "You cannot change your own role"));

        assertThat(reload("panelselfrole").getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldReturnBadRequest_whenRoleValueIsUnknown() throws Exception {
        UserEntity admin = newUser("panelbadroleadmin", Role.ADMIN);
        UserEntity member = newUser("panelbadrolemember", Role.USER);

        mockMvc.perform(post("/admin/users/" + member.getId() + "/role")
                        .param("role", "SUPERUSER")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(status().isBadRequest());

        assertThat(reload("panelbadrolemember").getRole()).isEqualTo(Role.USER);
    }

    @Test
    void shouldReturnForbidden_whenCsrfTokenIsMissing() throws Exception {
        UserEntity admin = newUser("panelcsrfadmin", Role.ADMIN);
        UserEntity member = newUser("panelcsrfmember", Role.USER);

        mockMvc.perform(post("/admin/users/" + member.getId() + "/enabled")
                        .param("enabled", "false")
                        .with(loginAs(admin)))
                .andExpect(status().isForbidden());

        assertThat(reload("panelcsrfmember").isEnabled()).isTrue();
    }

    @Test
    void shouldReturnNotFound_whenTargetUserDoesNotExist() throws Exception {
        UserEntity admin = newUser("panelunknownadmin", Role.ADMIN);

        mockMvc.perform(post("/admin/users/-1/role")
                        .param("role", "USER")
                        .with(csrf()).with(loginAs(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDenyManagementActions_whenUserIsRegular() throws Exception {
        UserEntity member = newUser("panelactor", Role.USER);
        UserEntity victim = newUser("panelvictim", Role.USER);

        mockMvc.perform(post("/admin/users/" + victim.getId() + "/enabled")
                        .param("enabled", "false")
                        .with(csrf()).with(loginAs(member)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/" + victim.getId() + "/role")
                        .param("role", "ADMIN")
                        .with(csrf()).with(loginAs(member)))
                .andExpect(status().isForbidden());

        assertThat(reload("panelvictim").isEnabled()).isTrue();
        assertThat(reload("panelvictim").getRole()).isEqualTo(Role.USER);
    }

    @Test
    void shouldDisplayUsersLinkOnly_whenUserIsAdministrator() throws Exception {
        UserEntity admin = newUser("panelnavadmin", Role.ADMIN);
        UserEntity member = newUser("panelnavmember", Role.USER);

        assertThat(body("/", admin)).contains("/admin/users");
        assertThat(body("/", member)).doesNotContain("/admin/users");
    }

    private UserEntity newUser(String username, Role role) {
        return userService.create(username, "password123", role);
    }

    private UserEntity reload(String username) {
        return userService.findByUsername(username).orElseThrow();
    }

    private RequestPostProcessor loginAs(UserEntity account) {
        return user(new UserDetailsImpl(account));
    }

    private String body(String url, UserEntity account) throws Exception {
        return mockMvc.perform(get(url).with(loginAs(account)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}