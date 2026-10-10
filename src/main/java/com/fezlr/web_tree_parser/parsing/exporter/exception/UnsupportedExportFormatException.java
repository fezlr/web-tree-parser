package com.fezlr.web_tree_parser.parsing.exporter.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedExportFormatException extends RuntimeException {

    public UnsupportedExportFormatException(String format) {
        super("Unsupported export format: " + format);
    }
}
