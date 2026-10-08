package com.fezlr.web_tree_parser.user.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String username) {
        super("Username is already taken: " + username);
    }
}
