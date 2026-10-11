package com.fezlr.web_tree_parser.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AppConstants {

    public static final int PAGE_SIZE = 20;
    public static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

}
