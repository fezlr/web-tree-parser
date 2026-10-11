package com.fezlr.web_tree_parser.history.controller;

import com.fezlr.web_tree_parser.auth.security.UserDetailsImpl;
import com.fezlr.web_tree_parser.history.dto.request.ParserHistoryFilterRequest;
import com.fezlr.web_tree_parser.history.dto.response.ParseHistoryResponse;
import com.fezlr.web_tree_parser.history.service.ParseHistoryService;
import com.fezlr.web_tree_parser.user.entity.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import static com.fezlr.web_tree_parser.common.AppConstants.NEWEST_FIRST;
import static com.fezlr.web_tree_parser.common.AppConstants.PAGE_SIZE;

@RequiredArgsConstructor
@Controller
public class ParseHistoryController {
    private final ParseHistoryService parseHistoryService;

    @GetMapping("/history")
    public String findHistories(@Valid @ModelAttribute("filter") ParserHistoryFilterRequest filter, BindingResult bindingResult, @RequestParam(defaultValue = "0") int page, @AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, NEWEST_FIRST);

        checkBindingResult(filter, bindingResult);

        Page<ParseHistoryResponse> result = bindingResult.hasErrors()
                ? Page.empty(pageable)
                : parseHistoryService.findHistories(userDetails.user(), filter, pageable);

        model.addAttribute("page", result);
        model.addAttribute("showAuthor", userDetails.user().getRole() == Role.ADMIN);

        return "history/list";
    }

    private void checkBindingResult(ParserHistoryFilterRequest filter, BindingResult bindingResult) {
        if (filter.getFrom() != null && filter.getTo() != null && filter.getFrom().isAfter(filter.getTo())) {
            bindingResult.rejectValue("to", "range.invalid", "End date must not be before start date");
        }
    }
}
