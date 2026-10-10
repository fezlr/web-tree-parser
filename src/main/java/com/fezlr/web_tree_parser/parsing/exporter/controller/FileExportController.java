package com.fezlr.web_tree_parser.parsing.exporter.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fezlr.web_tree_parser.auth.security.UserDetailsImpl;
import com.fezlr.web_tree_parser.parsing.exporter.dto.ExportFormat;
import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportFileRequest;
import com.fezlr.web_tree_parser.parsing.exporter.service.FileExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RequiredArgsConstructor
@Controller
public class FileExportController {
    private final FileExportService exportService;

    @GetMapping("/results/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id, @RequestParam(defaultValue = "json") String format, @AuthenticationPrincipal UserDetailsImpl userDetails) throws JsonProcessingException {
        ExportFileRequest file = exportService.export(id, userDetails.user(), ExportFormat.from(format));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.content());
    }
}
