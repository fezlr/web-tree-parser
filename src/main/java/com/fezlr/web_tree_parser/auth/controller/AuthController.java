package com.fezlr.web_tree_parser.auth.controller;

import com.fezlr.web_tree_parser.auth.dto.request.RegisterRequest;
import com.fezlr.web_tree_parser.auth.exception.RegistrationException;
import com.fezlr.web_tree_parser.auth.service.RegistrationService;
import com.fezlr.web_tree_parser.user.exception.UsernameAlreadyExistsException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Objects;

@RequiredArgsConstructor
@Controller
public class AuthController {
    private final RegistrationService registrationService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());

        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            registrationService.register(registerRequest);
        } catch (RegistrationException e) {
            bindingResult.rejectValue(e.getField(), "registration.failed", e.getMessage());
            return "auth/register";
        }

        return "redirect:/login?registered";
    }
}
