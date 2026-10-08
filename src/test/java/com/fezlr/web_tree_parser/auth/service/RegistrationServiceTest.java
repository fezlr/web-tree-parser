package com.fezlr.web_tree_parser.auth.service;

import com.fezlr.web_tree_parser.auth.dto.request.RegisterRequest;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.exception.UsernameAlreadyExistsException;
import com.fezlr.web_tree_parser.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void shouldRegisterUserWithUserRole_whenRequestIsValid() {
        RegisterRequest request = request("anton", "password123");
        UserEntity createdUser = UserEntity.builder()
                .username("anton")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
        when(userService.create("anton", "password123", Role.USER)).thenReturn(createdUser);

        UserEntity result = registrationService.register(request);

        assertThat(result).isSameAs(createdUser);
        verify(userService).create("anton", "password123", Role.USER);
    }

    @Test
    void shouldRegisterUserWithUserRole_whenRequestIsValidAndHasRoleAdmin() {
        RegisterRequest request = request("anton", "password123");
        UserEntity createdUser = UserEntity.builder()
                .username("anton")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .build();
        when(userService.create("anton", "password123", Role.USER)).thenReturn(createdUser);

        UserEntity result = registrationService.register(request);

        assertThat(result).isSameAs(createdUser);
        verify(userService).create("anton", "password123", Role.USER);
    }

    @Test
    void shouldThrowUsernameAlreadyExists_whenUsernameIsAlreadyTaken() {
        RegisterRequest request = request("anton", "password123");
        when(userService.create("anton", "password123", Role.USER))
                .thenThrow(new UsernameAlreadyExistsException("anton"));

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void shouldThrowIllegalArgumentException_whenUsernameIsEmpty() {
        RegisterRequest request = request("", "password123");
        when(userService.create("", "password123", Role.USER)).thenThrow(new IllegalArgumentException());

        assertThatThrownBy(() -> registrationService.register(request)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowIllegalArgumentException_whenPasswordIsEmpty() {
        RegisterRequest request = request("anton", "");
        when(userService.create("anton", "", Role.USER)).thenThrow(new IllegalArgumentException());

        assertThatThrownBy(() -> registrationService.register(request)).isInstanceOf(IllegalArgumentException.class);
    }

    private RegisterRequest request(String username, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setConfirmPassword(password);
        return request;
    }
}
