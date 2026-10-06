package com.fezlr.web_tree_parser.core.printer;

import com.fezlr.web_tree_parser.core.lexer.Lexer;
import com.fezlr.web_tree_parser.core.node.Node;
import com.fezlr.web_tree_parser.core.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TreePrinterTest {

    @Test
    void shouldRenderNestedTree_whenInput() {
        Node node = new Parser(new Lexer("{a, {b, c}, d}").tokenize()).parse();

        String expectedResult = """
                Set
                ├── a
                ├── Set
                │   ├── b
                │   └── c
                └── d
                """;

        assertEquals(expectedResult, new TreePrinter().render(node));
    }
}
