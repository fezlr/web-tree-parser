package com.fezlr.web_tree_parser.parsing.dto.result;

import com.fezlr.web_tree_parser.core.stats.Stats;
import lombok.Builder;

@Builder
public record ParseResult(

        String source,

        String tree,

        Stats stats
) {
}
