package com.fezlr.web_tree_parser.parsing.service;

import com.fezlr.web_tree_parser.parsing.dto.result.ParseResult;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserServiceTest {
    private final ParserService parserService = new ParserService();

    @Test
    void shouldParseSetIntoTreeAndStatistics_whenInputIsValid() {
        String input = "{1, {a, \"x\"}}";

        ParseResult result = parserService.parse(input);

        assertThat(result.source()).isEqualTo(input);
        assertThat(result.tree()).isEqualTo("Set\n├── 1\n└── Set\n    ├── a\n    └── x\n");
        assertThat(result.stats().totalElements()).isEqualTo(3);
        assertThat(result.stats().subsets()).isEqualTo(1);
        assertThat(result.stats().maxDepth()).isEqualTo(2);
    }

    @Test
    void shouldParseInputWithWindowsLineBreaks_whenInputIsValid() {
        ParseResult result = parserService.parse("{\r\n  1,\r\n  2\r\n}");

        assertThat(result.tree()).isEqualToNormalizingNewlines("Set\n├── 1\n└── 2\n");

    }

    @Test
    void shouldThrowIllegalArgumentException_whenInputIsNotValid() {
        assertThatThrownBy(() -> parserService.parse("{1,"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}