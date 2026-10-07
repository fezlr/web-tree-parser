package com.fezlr.web_tree_parser.auth.security;

import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {
    @Mock
    private UserService userService;
    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void shouldReturnUserDetailsWithRole_whenLoadingExistingUser() {
        UserEntity admin = UserEntity.builder()
                .username("admin")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .build();
        when(userService
                .findByUsername("admin"))
                .thenReturn(Optional.of(admin));

        UserDetails details = userDetailsService.loadUserByUsername("admin");
        List<String> authorities = details
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(authorities).containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldMarkUserDetailsAsDisabled_whenUserIsDisabled() {
        UserEntity user = UserEntity.builder()
                .username("anton")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
        user.setEnabled(false);
        when(userService.findByUsername("anton")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("anton");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void shouldThrowUsernameNotFoundException_whenUserDoesNotExist() {
        when(userService.findByUsername("anton")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("anton"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
