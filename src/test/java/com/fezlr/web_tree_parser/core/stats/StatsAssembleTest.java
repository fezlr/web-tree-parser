package com.fezlr.web_tree_parser.core.stats;

import com.fezlr.web_tree_parser.core.lexer.Lexer;
import com.fezlr.web_tree_parser.core.node.Node;
import com.fezlr.web_tree_parser.core.parser.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatsAssembleTest {

    @Test
    void shouldAssembleStats_whenInputIsTwoSets() {
        Node node = new Parser(new Lexer("{a, {1, \"c\"}, d}").tokenize()).parse();
        Stats stats = new StatsAssemble().assemble(node);

        assertEquals(4, stats.totalElements());
        assertEquals(1, stats.subsets());
        assertEquals(2, stats.maxDepth());
        assertEquals(1, stats.numberCount());
        assertEquals(2, stats.identifierCount());
        assertEquals(1, stats.stringCount());
        assertEquals(2, stats.setCount());
    }

    @Test
    void shouldAssembleStats_whenInputThreeSets() {
        Node node = new Parser(new Lexer("{a, {1, {123, 456}, \"c\"}, d}").tokenize()).parse();
        Stats stats = new StatsAssemble().assemble(node);

        assertEquals(6, stats.totalElements());
        assertEquals(2, stats.subsets());
        assertEquals(3, stats.maxDepth());
        assertEquals(3, stats.numberCount());
        assertEquals(2, stats.identifierCount());
        assertEquals(1, stats.stringCount());
        assertEquals(3, stats.setCount());
    }
}
