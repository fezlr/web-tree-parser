package com.fezlr.web_tree_parser.parsing.service;

import com.fezlr.web_tree_parser.core.lexer.Lexer;
import com.fezlr.web_tree_parser.core.node.Node;
import com.fezlr.web_tree_parser.core.parser.Parser;
import com.fezlr.web_tree_parser.core.printer.TreePrinter;
import com.fezlr.web_tree_parser.core.stats.StatsAssemble;
import com.fezlr.web_tree_parser.history.entity.ParseHistoryEntity;
import com.fezlr.web_tree_parser.history.mapper.ParseHistoryMapper;
import com.fezlr.web_tree_parser.history.service.ParseHistoryService;
import com.fezlr.web_tree_parser.parsing.dto.result.ParseResult;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ParserService {
    private final ParseHistoryService parseHistoryService;
    private final ParseHistoryMapper parseHistoryMapper;

    public Node parseTree(String input) {
        return new Parser(new Lexer(input).tokenize()).parse();
    }

    public ParseResult parse(String input) {
        Node tree = new Parser(new Lexer(input).tokenize()).parse();
        StatsAssemble statsAssemble = new StatsAssemble();
        TreePrinter treePrinter = new TreePrinter();

        return ParseResult.builder()
                .source(input)
                .tree(treePrinter.render(tree))
                .stats(statsAssemble.assemble(tree))
                .build();
    }

    public Long parseAndSave(String input, UserEntity userEntity) {
        ParseResult result = parse(input);

        ParseHistoryEntity entity = parseHistoryMapper.toEntity(userEntity, input, result.stats());
        ParseHistoryEntity savedEntity = parseHistoryService.save(entity);

        return savedEntity.getId();
    }
}
