package com.fezlr.web_tree_parser.user.service;

import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.exception.UsernameAlreadyExistsException;
import com.fezlr.web_tree_parser.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    @Test
    void shouldTrimAndEncode_whenUserIsCreated() {
        when(userRepository.existsByUsernameIgnoreCase("anton")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserEntity created = userService.create("  anton  ", "secret", Role.USER);

        assertThat(created.getUsername()).isEqualTo("anton");
        assertThat(created.getPasswordHash()).isEqualTo("encoded");
        assertThat(created.getRole()).isEqualTo(Role.USER);
        assertThat(created.isEnabled()).isTrue();
    }

    @Test
    void shouldThrowException_whenUsernameAlreadyExists() {
        when(userRepository.existsByUsernameIgnoreCase("anton")).thenReturn(true);

        assertThatThrownBy(() -> userService.create("anton", "secret", Role.USER))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldThrowUsernameAlreadyExists_whenUsernameIsDuplicate() {
        when(userRepository.existsByUsernameIgnoreCase("anton")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenThrow(new UsernameAlreadyExistsException("duplicate"));

        assertThatThrownBy(() -> userService.create("anton", "secret", Role.USER))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void shouldTrimInput_whenFindingByUsername() {
        UserEntity user = UserEntity.builder()
                .username("anton")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
        when(userRepository.findByUsernameIgnoreCase("anton")).thenReturn(Optional.of(user));

        assertThat(userService.findByUsername("  anton ")).containsSame(user);
    }

    @Test
    void shouldTrimInput_whenCheckingIfUsernameExists() {
        when(userRepository.existsByUsernameIgnoreCase("anton")).thenReturn(true);

        assertThat(userService.existsByUsername(" anton ")).isTrue();
    }
}
