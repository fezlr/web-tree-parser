package com.fezlr.web_tree_parser.parsing.exporter.dto.request;

public record ExportStatisticsRequest(
        int totalElements,

        int sets,

        int subsets,

        int maxDepth,

        int numbers,

        int identifiers,

        int strings
) {
}