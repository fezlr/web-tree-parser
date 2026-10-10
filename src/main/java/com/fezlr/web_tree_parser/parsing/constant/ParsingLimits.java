package com.fezlr.web_tree_parser.parsing.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParsingLimits {
    public static final int MAX_INPUT_LENGTH = 100_000;
    public static final int MAX_FILE_BYTES = 100_000*4;
}
