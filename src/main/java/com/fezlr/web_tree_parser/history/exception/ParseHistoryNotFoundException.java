package com.fezlr.web_tree_parser.history.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ParseHistoryNotFoundException extends RuntimeException {
    public ParseHistoryNotFoundException(Long id) {
        super("History not found for id: " + id);
    }
}
