package com.fezlr.web_tree_parser.admin.service;

import com.fezlr.web_tree_parser.admin.dto.response.UserInfoResponse;
import com.fezlr.web_tree_parser.admin.exception.AdminActionException;
import com.fezlr.web_tree_parser.admin.mapper.AdminUserMapper;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.exception.UserNotFoundException;
import com.fezlr.web_tree_parser.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    @Spy
    private AdminUserMapper adminUserMapper = new AdminUserMapper();

    @Test
    void shouldPromoteUser_whenUserIsRegular() {
        UserEntity actor = user(1L, "boss", Role.ADMIN, true);
        UserEntity target = user(2L, "member", Role.USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        UserInfoResponse summary = adminUserService.changeRole(2L, Role.ADMIN, actor);

        assertThat(target.getRole()).isEqualTo(Role.ADMIN);
        assertThat(summary.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldDemoteAdministrator_whenAnotherActiveAdministratorExists() {
        UserEntity actor = user(1L, "boss", Role.ADMIN, true);
        UserEntity target = user(2L, "second", Role.ADMIN, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByRoleAndEnabled(Role.ADMIN, true)).thenReturn(2L);

        adminUserService.changeRole(2L, Role.USER, actor);

        assertThat(target.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void shouldThrowAdminActionException_whenDemotingLastActiveAdministrator() {
        UserEntity staleActor = user(1L, "stale", Role.ADMIN, true);
        UserEntity target = user(2L, "last", Role.ADMIN, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByRoleAndEnabled(Role.ADMIN, true)).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.changeRole(2L, Role.USER, staleActor))
                .isInstanceOf(AdminActionException.class)
                .hasMessage("At least one active administrator is required");

        assertThat(target.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldDemoteAdministratorWithoutCheck_whenAdministratorIsAlreadyBlocked() {
        UserEntity actor = user(1L, "boss", Role.ADMIN, true);
        UserEntity target = user(2L, "dormant", Role.ADMIN, false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        adminUserService.changeRole(2L, Role.USER, actor);

        assertThat(target.getRole()).isEqualTo(Role.USER);
        verify(userRepository, never()).countByRoleAndEnabled(any(), anyBoolean());
    }

    @Test
    void shouldDoNothing_whenTargetRoleIsAlreadySet() {
        UserEntity actor = user(1L, "boss", Role.ADMIN, true);
        UserEntity target = user(2L, "member", Role.USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        adminUserService.changeRole(2L, Role.USER, actor);

        assertThat(target.getRole()).isEqualTo(Role.USER);
        verify(userRepository, never()).countByRoleAndEnabled(any(), anyBoolean());
    }

    @Test
    void shouldThrowAdminActionException_whenChangingOwnRole() {
        UserEntity actor = user(1L, "boss", Role.ADMIN, true);

        assertThatThrownBy(() -> adminUserService.changeRole(1L, Role.USER, actor))
                .isInstanceOf(AdminActionException.class)
                .hasMessage("You cannot change your own role");

        assertThat(actor.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldBlockUser_whenUserIsEnabled() {
        UserEntity actor = user(1L, "boss", Role.ADMIN, true);
        UserEntity target = user(2L, "member", Role.USER, true);

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        UserInfoResponse response = adminUserService.setEnabled(2L, false, actor);

        assertThat(target.isEnabled()).isFalse();
        assertThat(response.enabled()).isFalse();
    }

    @Test
    void shouldUnblockUser_whenUserIsDisabled() {
        UserEntity actor = user(1L, "boss", Role.ADMIN, true);
        UserEntity target = user(2L, "member", Role.USER, false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        UserInfoResponse response = adminUserService.setEnabled(2L, true, actor);

        assertThat(target.isEnabled()).isTrue();
        assertThat(response.enabled()).isTrue();
    }


    @Test
    void shouldBlockAdministrator_whenAnotherActiveAdministratorExists() {
        UserEntity actor = user(1L, "boss", Role.ADMIN, true);
        UserEntity target = user(2L, "second", Role.ADMIN, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByRoleAndEnabled(Role.ADMIN, true)).thenReturn(2L);

        adminUserService.setEnabled(2L, false, actor);

        assertThat(target.isEnabled()).isFalse();
    }

    @Test
    void shouldThrowAdminActionException_whenBlockingLastActiveAdministrator() {
        UserEntity staleActor = user(1L, "stale", Role.ADMIN, true);
        UserEntity target = user(2L, "last", Role.ADMIN, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByRoleAndEnabled(Role.ADMIN, true)).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.setEnabled(2L, false, staleActor))
                .isInstanceOf(AdminActionException.class)
                .hasMessage("At least one active administrator is required");

        assertThat(target.isEnabled()).isTrue();
    }

    @Test
    void shouldDoNothing_whenUserAlreadyInRequestedState() {
        UserEntity actor = user(1L, "boss", Role.ADMIN, true);
        UserEntity target = user(2L, "member", Role.USER, false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        adminUserService.setEnabled(2L, false, actor);

        assertThat(target.isEnabled()).isFalse();
        verify(userRepository, never()).countByRoleAndEnabled(any(), anyBoolean());
    }

    @Test
    void shouldThrowAdminActionException_whenBlockingOwnAccount() {
        UserEntity actor = user(1L, "boss", Role.ADMIN, true);

        assertThatThrownBy(() -> adminUserService.setEnabled(1L, false, actor))
                .isInstanceOf(AdminActionException.class)
                .hasMessage("You cannot block or unblock your own account");

        assertThat(actor.isEnabled()).isTrue();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void shouldThrowUserNotFoundException_whenUserNotFound() {
        UserEntity actor = user(1L, "boss", Role.ADMIN, true);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.changeRole(99L, Role.ADMIN, actor))
                .isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> adminUserService.setEnabled(99L, false, actor))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldReturnUserInfoResponseList_whenUsersExist() {
        Pageable pageable = PageRequest.of(0, 20);
        UserEntity first = user(1L, "first", Role.ADMIN, true);
        UserEntity second = user(2L, "second", Role.USER, false);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));

        Page<UserInfoResponse> page = adminUserService.findAll(pageable);

        assertThat(page.getContent()).extracting(UserInfoResponse::username).containsExactly("first", "second");
        assertThat(page.getContent()).extracting(UserInfoResponse::enabled).containsExactly(true, false);
    }

    private UserEntity user(Long id, String username, Role role, boolean enabled) {
        UserEntity user = UserEntity.builder().username(username).passwordHash("hash").role(role).build();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", Instant.now());
        user.setEnabled(enabled);
        return user;
    }
}
