package com.fezlr.web_tree_parser.parsing.exporter.dto;

import com.fezlr.web_tree_parser.parsing.exporter.exception.UnsupportedExportFormatException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ExportFormat {
    JSON("json", "application/json"),
    XML("xml", "application/xml");

    private final String extension;
    private final String contentType;

    public static ExportFormat from(String value) {
        return Arrays.stream(values())
                .filter(format -> format.extension.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new UnsupportedExportFormatException(value));
    }
}
