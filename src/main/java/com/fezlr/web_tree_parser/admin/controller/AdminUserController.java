package com.fezlr.web_tree_parser.admin.controller;

import com.fezlr.web_tree_parser.admin.dto.response.UserInfoResponse;
import com.fezlr.web_tree_parser.admin.exception.AdminActionException;
import com.fezlr.web_tree_parser.admin.service.AdminUserService;
import com.fezlr.web_tree_parser.auth.security.UserDetailsImpl;
import com.fezlr.web_tree_parser.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.fezlr.web_tree_parser.common.AppConstants.NEWEST_FIRST;
import static com.fezlr.web_tree_parser.common.AppConstants.PAGE_SIZE;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping
    public String findAll(@RequestParam(defaultValue = "0") int page, @AuthenticationPrincipal UserDetailsImpl principal, Model model) {
        model.addAttribute("page", adminUserService.findAll(PageRequest.of(Math.max(page, 0), PAGE_SIZE, NEWEST_FIRST)));
        model.addAttribute("roles", Role.values());
        model.addAttribute("currentUserId", principal.user().getId());

        return "admin/users";
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role, @RequestParam(defaultValue = "0") int page, @AuthenticationPrincipal UserDetailsImpl principal, RedirectAttributes redirect) {
        try {
            UserInfoResponse updated = adminUserService.changeRole(id, role, principal.user());
            redirect.addFlashAttribute("message", "Role of " + updated.username() + " is now " + updated.role());
        } catch (AdminActionException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }

        redirect.addAttribute("page", Math.max(page, 0));

        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/enabled")
    public String setEnabled(@PathVariable Long id, @RequestParam boolean enabled, @RequestParam(defaultValue = "0") int page, @AuthenticationPrincipal UserDetailsImpl principal, RedirectAttributes redirect) {
        try {
            UserInfoResponse updated = adminUserService.setEnabled(id, enabled, principal.user());
            redirect.addFlashAttribute("message",
                    updated.username() + (updated.enabled() ? " is unblocked" : " is blocked"));
        } catch (AdminActionException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }

        redirect.addAttribute("page", Math.max(page, 0));

        return "redirect:/admin/users";
    }
}
