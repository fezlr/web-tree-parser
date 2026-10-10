package com.fezlr.web_tree_parser.parsing.controller;

import com.fezlr.web_tree_parser.auth.security.UserDetailsImpl;
import com.fezlr.web_tree_parser.history.dto.response.ParseHistoryResponse;
import com.fezlr.web_tree_parser.history.service.ParseHistoryService;
import com.fezlr.web_tree_parser.parsing.dto.request.ParseRequest;
import com.fezlr.web_tree_parser.parsing.importer.exception.InvalidImportFileException;
import com.fezlr.web_tree_parser.parsing.importer.service.FileImportService;
import com.fezlr.web_tree_parser.parsing.service.ParserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class ParserController {
    private final ParserService parserService;
    private final ParseHistoryService parseHistoryService;
    private final FileImportService fileImportService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("parseRequest", new ParseRequest());

        return "parser/index";
    }

    @PostMapping("/parse")
    public String parse(@Valid @ModelAttribute ParseRequest request, BindingResult bindingResult, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (bindingResult.hasErrors()) {
            return "parser/index";
        }

        try {
            Long id = parserService.parseAndSave(request.getInput(), userDetails.user());
            return "redirect:/results/" + id;
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("input", "input.invalid", e.getMessage());
            return "parser/index";
        }
    }

    @PostMapping("/parse/file")
    public String parseFile(@RequestParam(value = "file", required = false) MultipartFile file, @AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        try {
            String input = fileImportService.read(file);
            Long id = parserService.parseAndSave(input, userDetails.user());

            return "redirect:/results/" + id;
        } catch (InvalidImportFileException | IllegalArgumentException e) {
            model.addAttribute("parseRequest", new ParseRequest());
            model.addAttribute("fileError", e.getMessage());

            return "parser/index";
        }
    }

    @GetMapping("/results/{id}")
    public String result(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        ParseHistoryResponse response = parseHistoryService.findAccessibleHistory(id, userDetails.user());

        model.addAttribute("entry", response);
        model.addAttribute("result", parserService.parse(response.source()));

        return "parser/result";
    }
}
