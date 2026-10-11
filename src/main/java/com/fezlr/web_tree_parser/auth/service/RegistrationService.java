package com.fezlr.web_tree_parser.auth.service;

import com.fezlr.web_tree_parser.auth.dto.request.RegisterRequest;
import com.fezlr.web_tree_parser.auth.exception.RegistrationException;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.exception.UsernameAlreadyExistsException;
import com.fezlr.web_tree_parser.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Objects;

@RequiredArgsConstructor
@Service
public class RegistrationService {
    private final UserService userService;

    public UserEntity register(RegisterRequest request) {
        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            throw new RegistrationException("confirmPassword", "Passwords do not match");
        }

        try {
            return userService.create(request.getUsername(), request.getPassword(), Role.USER);
        } catch (UsernameAlreadyExistsException e) {
            throw new RegistrationException("username", "Username is already taken");
        }
    }
}
