package com.fezlr.web_tree_parser.parsing.exporter.service;

import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportDocumentRequest;
import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportNodeRequest;
import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportStatisticsRequest;
import org.springframework.stereotype.Component;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;

@Component
public class XmlExportService {
    private static final int REPLACEMENT_CHARACTER = 0xFFFD;
    private static final String INDENT = "  ";

    public byte[] write(ExportDocumentRequest document) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output, "UTF-8");

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeCharacters("\n");
            writer.writeStartElement("parseResult");
            writer.writeAttribute("id", String.valueOf(document.id()));
            writer.writeAttribute("author", clean(document.author()));
            writer.writeAttribute("createdAt", clean(document.createdAt()));

            indent(writer, 1);
            writer.writeStartElement("source");
            writer.writeCharacters(clean(document.source()));
            writer.writeEndElement();

            indent(writer, 1);
            writer.writeStartElement("tree");
            writeNode(writer, document.tree(), 2);
            indent(writer, 1);
            writer.writeEndElement();

            writeStatistics(writer, document.statistics());

            indent(writer, 0);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Could not write XML export", e);
        }

        return output.toByteArray();
    }

    private void writeNode(XMLStreamWriter writer, ExportNodeRequest node, int level) throws XMLStreamException {
        indent(writer, level);

        if (node.elements() == null) {
            writer.writeStartElement(node.type());
            writer.writeCharacters(clean(node.value()));
            writer.writeEndElement();
            return;
        }

        if (node.elements().isEmpty()) {
            writer.writeEmptyElement(node.type());
            return;
        }

        writer.writeStartElement(node.type());
        for (ExportNodeRequest child : node.elements()) {
            writeNode(writer, child, level + 1);
        }
        indent(writer, level);
        writer.writeEndElement();
    }

    private void writeStatistics(XMLStreamWriter writer, ExportStatisticsRequest statistics) throws XMLStreamException {
        indent(writer, 1);
        writer.writeEmptyElement("statistics");
        writer.writeAttribute("totalElements", String.valueOf(statistics.totalElements()));
        writer.writeAttribute("sets", String.valueOf(statistics.sets()));
        writer.writeAttribute("subsets", String.valueOf(statistics.subsets()));
        writer.writeAttribute("maxDepth", String.valueOf(statistics.maxDepth()));
        writer.writeAttribute("numbers", String.valueOf(statistics.numbers()));
        writer.writeAttribute("identifiers", String.valueOf(statistics.identifiers()));
        writer.writeAttribute("strings", String.valueOf(statistics.strings()));
    }

    private void indent(XMLStreamWriter writer, int level) throws XMLStreamException {
        writer.writeCharacters("\n" + INDENT.repeat(level));
    }

    private String clean(String value) {
        StringBuilder builder = new StringBuilder(value.length());

        value.codePoints().forEach(codePoint ->
                builder.appendCodePoint(isXmlCharacter(codePoint) ? codePoint : REPLACEMENT_CHARACTER));

        return builder.toString();
    }

    private boolean isXmlCharacter(int codePoint) {
        return codePoint == 0x9
                || codePoint == 0xA
                || codePoint == 0xD
                || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
    }
}
