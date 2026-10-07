package com.fezlr.web_tree_parser.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(

        @NotBlank String username,

        @NotBlank String password

) {
}
