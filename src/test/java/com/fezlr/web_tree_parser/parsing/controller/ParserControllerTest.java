package com.fezlr.web_tree_parser.parsing.controller;

import com.fezlr.web_tree_parser.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ParserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void shouldShowForm_whenAccessingRoot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("parser/index"))
                .andExpect(model().attributeExists("parseRequest"))
                .andExpect(model().attributeDoesNotExist("result"));
    }

    @Test
    @WithMockUser
    void shouldParseTree_whenInputIsValid() throws Exception {
        mockMvc.perform(post("/parse").with(csrf())
                        .param("input", "{1, {a, \"x\"}}"))
                .andExpect(status().isOk())
                .andExpect(view().name("parser/index"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().hasNoErrors());
    }

    @Test
    @WithMockUser
    void shouldShowError_whenInputIsNotValid() throws Exception {
        mockMvc.perform(post("/parse").with(csrf())
                        .param("input", "{1,"))
                .andExpect(status().isOk())
                .andExpect(view().name("parser/index"))
                .andExpect(model().attributeHasFieldErrors("parseRequest", "input"))
                .andExpect(model().attributeDoesNotExist("result"));
    }

    @Test
    @WithMockUser
    void shouldShowError_whenInputIsBlank() throws Exception {
        mockMvc.perform(post("/parse").with(csrf())
                        .param("input", "   "))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("parseRequest", "input"))
                .andExpect(model().attributeDoesNotExist("result"));
    }


    @Test
    @WithMockUser
    void shouldShowError_whenInputIsTooLong() throws Exception {
        mockMvc.perform(post("/parse").with(csrf())
                        .param("input", "a".repeat(100_001)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("parseRequest", "input"))
                .andExpect(model().attributeDoesNotExist("result"));
    }

    @Test
    @WithMockUser
    void shouldParseTree_whenInputIsAtMaxLength() throws Exception {
        String maxValidInput = "{" + "a,".repeat(49_998) + "a}";

        mockMvc.perform(post("/parse").with(csrf())
                        .param("input", maxValidInput))
                .andExpect(status().isOk())
                .andExpect(view().name("parser/index"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().hasNoErrors());
    }

    @Test
    void shouldRedirectToLogin_whenUserIsAnonymous() throws Exception {
        mockMvc.perform(post("/parse").with(csrf())
                        .param("input", "{1}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser
    void shouldRedirectToLogin_whenUserLogsOut() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }
}
