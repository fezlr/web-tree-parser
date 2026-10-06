package com.fezlr.web_tree_parser.core.lexer;

public record Token(

        TokenType tokenType,

        String lexeme,

        int position

) {
}
