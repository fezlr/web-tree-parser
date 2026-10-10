package com.fezlr.web_tree_parser.parsing.exporter.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExportNodeRequest(

        String type,

        String value,

        List<ExportNodeRequest> elements
) {
}
