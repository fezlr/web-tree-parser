package com.fezlr.web_tree_parser.config;

import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {
    private AdminInitializer initializer;
    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        AdminProperties properties = new AdminProperties("admin", "admin-password");
        initializer = new AdminInitializer(userService, properties);
    }

    @Test
    void shouldCreateDefaultAdministrator_whenMissingInDatabase() {
        when(userService.existsByUsername("admin")).thenReturn(false);

        initializer.run(new DefaultApplicationArguments());

        verify(userService).create("admin", "admin-password", Role.ADMIN);
    }

    @Test
    void shouldNotCreateAdmin_whenAdminIsAlreadyPresent() {
        when(userService.existsByUsername("admin")).thenReturn(true);

        initializer.run(new DefaultApplicationArguments());

        verify(userService, never()).create(any(), any(), any());
    }
}
