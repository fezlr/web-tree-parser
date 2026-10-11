package com.fezlr.web_tree_parser.admin.service;

import com.fezlr.web_tree_parser.admin.dto.response.UserInfoResponse;
import com.fezlr.web_tree_parser.admin.exception.AdminActionException;
import com.fezlr.web_tree_parser.admin.mapper.AdminUserMapper;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.exception.UserNotFoundException;
import com.fezlr.web_tree_parser.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.time.ZoneOffset;

@RequiredArgsConstructor
@Service
public class AdminUserService {
    private static final ZoneId ZONE = ZoneOffset.UTC;
    private final UserRepository userRepository;
    private final AdminUserMapper adminUserMapper;

    @Transactional(readOnly = true)
    public Page<UserInfoResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(user -> adminUserMapper.toResponse(user, ZONE));
    }

    @Transactional
    public UserInfoResponse changeRole(Long targetId, Role role, UserEntity userEntity) {
        if (userEntity.getId().equals(targetId)) {
            throw new AdminActionException("You cannot change your own role");
        }

        UserEntity target = findById(targetId);

        if (target.getRole() != role) {
            if (isActiveAdministrator(target)) {
                requireAnotherActiveAdministrator();
            }
            target.setRole(role);
        }

        target.setRole(role);

        return adminUserMapper.toResponse(target, ZONE);
    }

    @Transactional
    public UserInfoResponse setEnabled(Long targetId, boolean enabled, UserEntity userEntity) {
        if (userEntity.getId().equals(targetId)) {
            throw new AdminActionException("You cannot block or unblock your own account");
        }

        UserEntity target = findById(targetId);

        if (target.isEnabled() != enabled) {
            if (!enabled && isActiveAdministrator(target)) {
                requireAnotherActiveAdministrator();
            }
            target.setEnabled(enabled);
        }

        return adminUserMapper.toResponse(target, ZONE);
    }

    private UserEntity findById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private boolean isActiveAdministrator(UserEntity user) {
        return user.getRole() == Role.ADMIN && user.isEnabled();
    }

    private void requireAnotherActiveAdministrator() {
        if (userRepository.countByRoleAndEnabled(Role.ADMIN, true) <= 1) {
            throw new AdminActionException("At least one active administrator is required");
        }
    }
}
