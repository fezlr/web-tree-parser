package com.fezlr.web_tree_parser.auth.controller;

import com.fezlr.web_tree_parser.TestcontainersConfiguration;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.repository.UserRepository;
import com.fezlr.web_tree_parser.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    void shouldShowRegistrationForm_whenRequestIsGet() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void shouldRegisterNewUserWithUserRole_whenRequestIsValid() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "anton")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        UserEntity savedUser = userRepository.findByUsernameIgnoreCase("anton").orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("password123");
    }

    @Test
    void shouldIgnoreRoleParameter_whenSentByClientDuringRegistration() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "anton")
                        .param("password", "password123")
                        .param("confirmPassword", "password123")
                        .param("role", "ADMIN"))
                .andExpect(redirectedUrl("/login?registered"));

        UserEntity savedUser = userRepository.findByUsernameIgnoreCase("anton").orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void shouldAuthenticateSuccessfully_whenRegisteredUserLogsIn() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "anton")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(redirectedUrl("/login?registered"));

        mockMvc.perform(formLogin().user("anton").password("password123"))
                .andExpect(authenticated().withUsername("anton").withRoles("USER"));
    }

    @Test
    void shouldRejectRegistration_whenPasswordsMismatch() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "anton")
                        .param("password", "password123")
                        .param("confirmPassword", "different123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "confirmPassword"));

        assertThat(userRepository.existsByUsernameIgnoreCase("anton")).isFalse();
    }

    @Test
    void shouldRejectRegistration_whenPasswordIsShort() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "anton")
                        .param("password", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password"));

        assertThat(userRepository.existsByUsernameIgnoreCase("anton")).isFalse();
    }

    @Test
    void shouldRejectRegistration_whenUsernameContainsIllegalCharacters() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "anton%$!")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "username"));
    }

    @Test
    void shouldRejectRegistration_whenUsernameIsAlreadyTakenIgnoringCase() throws Exception {
        userService.create("Anton", "password123", Role.USER);

        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "anton")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerRequest", "username"));
    }

    @Test
    void shouldReturnForbidden_whenRegistrationIsMissingCsrfToken() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "anton")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsByUsernameIgnoreCase("anton")).isFalse();
    }
}
