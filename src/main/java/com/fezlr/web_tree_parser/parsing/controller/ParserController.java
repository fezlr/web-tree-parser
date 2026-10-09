package com.fezlr.web_tree_parser.parsing.controller;

import com.fezlr.web_tree_parser.parsing.dto.request.ParseRequest;
import com.fezlr.web_tree_parser.parsing.service.ParserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class ParserController {
    private final ParserService parserService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("parseRequest", new ParseRequest());

        return "parser/index";
    }

    @PostMapping("/parse")
    public String parse(@Valid @ModelAttribute ParseRequest request, BindingResult bindingResult, Model model) {

        if (bindingResult.hasErrors()) {
            return "parser/index";
        }

        try {
            model.addAttribute("result", parserService.parse(request.getInput()));
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("input", "input.invalid", e.getMessage());
        }

        return "parser/index";
    }
}
