package com.fezlr.web_tree_parser.parsing.exporter.service;

import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportDocumentRequest;
import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportNodeRequest;
import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportStatisticsRequest;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class XmlExporterServiceTest {
    private final XmlExportService writer = new XmlExportService();

    @Test
    void shouldWriteMetadataAndSource_whenDocumentIsValid() throws Exception {
        Document xml = parse(writer.write(document("{1, {a}}", sampleTree())));

        assertThat(xpath(xml, "/parseResult/@id")).isEqualTo("7");
        assertThat(xpath(xml, "/parseResult/@author")).isEqualTo("alice");
        assertThat(xpath(xml, "/parseResult/@createdAt")).isEqualTo("2026-10-09T12:00:00+03:00");
        assertThat(xpath(xml, "/parseResult/source")).isEqualTo("{1, {a}}");
    }

    @Test
    void shouldWriteTreeStructure_whenTreeHasNestedNodes() throws Exception {
        Document xml = parse(writer.write(document("{1, {a}}", sampleTree())));

        assertThat(xpath(xml, "/parseResult/tree/set/number")).isEqualTo("1");
        assertThat(xpath(xml, "/parseResult/tree/set/set/identifier")).isEqualTo("a");
    }

    @Test
    void shouldWriteStatistics_whenDocumentHasMetrics() throws Exception {
        Document xml = parse(writer.write(document("{1, {a}}", sampleTree())));

        assertThat(xpath(xml, "/parseResult/statistics/@totalElements")).isEqualTo("2");
        assertThat(xpath(xml, "/parseResult/statistics/@subsets")).isEqualTo("1");
        assertThat(xpath(xml, "/parseResult/statistics/@maxDepth")).isEqualTo("2");
    }

    @Test
    void shouldWriteEmptySetAsEmptyElement_whenElementsCollectionIsEmpty() throws Exception {
        ExportNodeRequest empty = ExportNodeRequest.builder()
                .type("set")
                .value(null)
                .elements(List.of())
                .build();

        Document xml = parse(writer.write(document("{}", empty)));

        assertThat(xpath(xml, "count(/parseResult/tree/set)")).isEqualTo("1");
        assertThat(xpath(xml, "count(/parseResult/tree/set/*)")).isEqualTo("0");
    }

    @Test
    void shouldEscapeMarkupCharacters_whenTextContainsXmlSpecialSymbols() throws Exception {
        ExportNodeRequest tree = ExportNodeRequest.builder()
                .type("set")
                .value(null)
                .elements(List.of(new ExportNodeRequest("string", "<a & b>", null)))
                .build();

        Document xml = parse(writer.write(document("{\"<a & b>\"}", tree)));

        assertThat(xpath(xml, "/parseResult/source")).isEqualTo("{\"<a & b>\"}");
        assertThat(xpath(xml, "/parseResult/tree/set/string")).isEqualTo("<a & b>");
    }

    @Test
    void shouldReplaceCharacters_whenCharactersAreInvalidInXmlSpecification() throws Exception {
        ExportNodeRequest tree = ExportNodeRequest.builder()
                .type("set")
                .value(null)
                .elements(List.of(
                        new ExportNodeRequest("string", "a\u0001b", null),
                        new ExportNodeRequest("string", "x\uD800y", null)
                ))
                .build();

        Document xml = parse(writer.write(document("{}", tree)));

        assertThat(xpath(xml, "/parseResult/tree/set/string[1]")).isEqualTo("a\uFFFDb");
        assertThat(xpath(xml, "/parseResult/tree/set/string[2]")).isEqualTo("x\uFFFDy");
    }

    @Test
    void shouldKeepText_whenContentContainsNonLatinTextOrSurrogatePairs() throws Exception {
        String text = "привет Ω \uD83D\uDE00";
        ExportNodeRequest tree = ExportNodeRequest.builder()
                .type("set")
                .value(null)
                .elements(List.of(new ExportNodeRequest("string", text, null)))
                .build();

        Document xml = parse(writer.write(document("{}", tree)));

        assertThat(xpath(xml, "/parseResult/tree/set/string")).isEqualTo(text);
    }

    @Test
    void shouldDeclareUtf8Encoding_whenXmlHeaderIsGenerated() {
        String xml = new String(writer.write(document("{1, {a}}", sampleTree())), StandardCharsets.UTF_8);

        assertThat(xml).startsWith("<?xml").contains("encoding=\"UTF-8\"");
    }

    private ExportNodeRequest sampleTree() {
        return ExportNodeRequest.builder()
                .type("set")
                .value(null)
                .elements(List.of(
                        new ExportNodeRequest("number", "1", null),
                        new ExportNodeRequest("set", null, List.of(new ExportNodeRequest("identifier", "a", null)))
                ))
                .build();
    }

    private ExportDocumentRequest document(String source, ExportNodeRequest tree) {
        return new ExportDocumentRequest(7L, "alice", source, tree,
                new ExportStatisticsRequest(2, 2, 1, 2, 1, 1, 0), "2026-10-09T12:00:00+03:00");
    }

    private Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        return factory
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml));
    }

    private String xpath(Document document, String expression) throws Exception {
        return XPathFactory
                .newInstance()
                .newXPath()
                .evaluate(expression, document);
    }
}