package com.fezlr.web_tree_parser.parsing.exporter.dto.request;

import lombok.Builder;

@Builder
public record ExportDocumentRequest(

        Long id,

        String author,

        String source,

        ExportNodeRequest tree,

        ExportStatisticsRequest statistics,

        String createdAt
) {
}
