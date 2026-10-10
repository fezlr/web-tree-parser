package com.fezlr.web_tree_parser.parsing.exporter.dto.request;

import lombok.Builder;

@Builder
public record ExportFileRequest(

        String fileName,

        String contentType,

        byte[] content

) {
}
