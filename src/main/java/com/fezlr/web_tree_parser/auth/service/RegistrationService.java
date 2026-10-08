package com.fezlr.web_tree_parser.auth.service;

import com.fezlr.web_tree_parser.auth.dto.request.RegisterRequest;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RegistrationService {
    private final UserService userService;

    public UserEntity register(RegisterRequest request) {
        if(!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        return userService.create(request.getUsername(), request.getPassword(), Role.USER);
    }
}
