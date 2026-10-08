package com.fezlr.web_tree_parser.user.service;

import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.exception.UsernameAlreadyExistsException;
import com.fezlr.web_tree_parser.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@AllArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserEntity create(String username, String password, Role role) {
        String trimmedUsername = username.strip();

        if (userRepository.existsByUsernameIgnoreCase(trimmedUsername)) {
            throw new UsernameAlreadyExistsException(trimmedUsername);
        }

        UserEntity user = UserEntity.builder()
                .username(trimmedUsername)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository
                .findByUsernameIgnoreCase(username.strip());
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository
                .existsByUsernameIgnoreCase(username.strip());
    }
}
