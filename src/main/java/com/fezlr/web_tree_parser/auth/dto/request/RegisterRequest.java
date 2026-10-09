package com.fezlr.web_tree_parser.auth.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 32, message = "Username must be between 2 and 32 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_а-яА-ЯёЁ]+$", message = "Username can only contain letters, numbers and underscores")
    String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    String password;

    @NotBlank(message = "Confirm the password")
    String confirmPassword;
}
