package com.fezlr.web_tree_parser.history.controller;

import com.fezlr.web_tree_parser.auth.security.UserDetailsImpl;
import com.fezlr.web_tree_parser.history.service.ParseHistoryService;
import com.fezlr.web_tree_parser.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller
public class ParseHistoryController {
    private static final int PAGE_SIZE = 20;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");
    private final ParseHistoryService parseHistoryService;

    @GetMapping("/history")
    public String findHistories(@RequestParam(defaultValue = "0") int page, @AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, NEWEST_FIRST);

        model.addAttribute("page", parseHistoryService.findHistories(userDetails.user(), pageable));
        model.addAttribute("showAuthor", userDetails.user().getRole() == Role.ADMIN);

        return "history/list";
    }
}
