package com.fezlr.web_tree_parser.parsing.service;

import com.fezlr.web_tree_parser.core.lexer.Lexer;
import com.fezlr.web_tree_parser.core.node.Node;
import com.fezlr.web_tree_parser.core.parser.Parser;
import com.fezlr.web_tree_parser.core.printer.TreePrinter;
import com.fezlr.web_tree_parser.core.stats.StatsAssemble;
import com.fezlr.web_tree_parser.parsing.dto.result.ParseResult;
import org.springframework.stereotype.Service;

@Service
public class ParserService {
    private final TreePrinter treePrinter = new TreePrinter();
    private final StatsAssemble statsAssemble = new StatsAssemble();

    public ParseResult parse(String input) {
        Node tree = new Parser(new Lexer(input).tokenize()).parse();

        return ParseResult.builder()
                .source(input)
                .tree(treePrinter.render(tree))
                .stats(statsAssemble.assemble(tree))
                .build();
    }
}
