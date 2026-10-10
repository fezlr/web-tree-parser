package com.fezlr.web_tree_parser.parsing.exporter.mapper;

import com.fezlr.web_tree_parser.core.node.*;
import com.fezlr.web_tree_parser.history.dto.response.ParseHistoryResponse;
import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportDocumentRequest;
import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportNodeRequest;
import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportStatisticsRequest;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Component
public class FileExportMapper {
    public ExportDocumentRequest toDocument(ParseHistoryResponse response, ZoneId zone, Node node) {
        ExportStatisticsRequest statistics = new ExportStatisticsRequest(
                response.totalElements(),
                response.setCount(),
                response.subsets(),
                response.maxDepth(),
                response.numberCount(),
                response.identifierCount(),
                response.stringCount()
        );

        String createdAt = Objects.requireNonNullElse(response.createdAt(), LocalDateTime.now())
                .atZone(zone)
                .toOffsetDateTime()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        return ExportDocumentRequest.builder()
                .id(response.id())
                .author(response.author())
                .source(response.source())
                .tree(toNode(node))
                .statistics(statistics)
                .createdAt(createdAt)
                .build();
    }

    public ExportNodeRequest toNode(Node node) {
        return switch (node) {
            case SetNode(List<Node> elements) -> ExportNodeRequest.builder()
                    .type("set")
                    .elements(elements.stream().map(this::toNode).toList())
                    .build();

            case NumberNode number -> ExportNodeRequest.builder()
                    .type("number")
                    .value(number.value())
                    .build();

            case IdentifierNode identifier -> ExportNodeRequest.builder()
                    .type("identifier")
                    .value(identifier.value())
                    .build();

            case StringNode string -> ExportNodeRequest.builder()
                    .type("string")
                    .value(string.value())
                    .build();

            default -> throw new IllegalStateException("Unknown node type: " + node);
        };
    }
}
