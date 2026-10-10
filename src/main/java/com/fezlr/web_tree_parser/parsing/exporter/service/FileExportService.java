package com.fezlr.web_tree_parser.parsing.exporter.service;

import com.fezlr.web_tree_parser.core.node.*;
import com.fezlr.web_tree_parser.history.dto.response.ParseHistoryResponse;
import com.fezlr.web_tree_parser.history.service.ParseHistoryService;
import com.fezlr.web_tree_parser.parsing.exporter.dto.ExportFormat;
import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportDocumentRequest;
import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportFileRequest;
import com.fezlr.web_tree_parser.parsing.exporter.mapper.FileExportMapper;
import com.fezlr.web_tree_parser.parsing.service.ParserService;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class FileExportService {
    private static final ZoneId ZONE = ZoneOffset.UTC;

    private final FileExportMapper fileExportMapper;
    private final ParseHistoryService parseHistoryService;
    private final ParserService parserService;
    private final JsonMapper jsonMapper;
    private final XmlExportService xmlExportService;

    public ExportFileRequest export(Long id, UserEntity userEntity, ExportFormat format) throws JsonProcessingException {
        ParseHistoryResponse response = parseHistoryService.findAccessibleHistory(id, userEntity);
        Node node = parserService.parseTree(response.source());

        ExportDocumentRequest document = fileExportMapper.toDocument(response, ZONE, node);

        byte[] content = switch (format) {
            case JSON -> jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(document);
            case XML -> xmlExportService.write(document);
        };

        return ExportFileRequest.builder()
                .fileName("parse-result-" + id + "." + format.getExtension())
                .contentType(format.getContentType())
                .content(content)
                .build();
    }
}
