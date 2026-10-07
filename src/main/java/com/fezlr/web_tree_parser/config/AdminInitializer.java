package com.fezlr.web_tree_parser.config;

import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;


@NullMarked
@Slf4j
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
@Component
public class AdminInitializer implements ApplicationRunner {
    private final UserService userService;
    private final AdminProperties adminProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (userService.existsByUsername(adminProperties.username())) {
            return;
        }

        userService.create(adminProperties.username(), adminProperties.password(), Role.ADMIN);
        log.info("Administrator has been created with name: {}", adminProperties.username());
    }
}
