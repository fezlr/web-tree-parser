package com.fezlr.web_tree_parser.parsing.exporter.dto;

import com.fezlr.web_tree_parser.parsing.exporter.exception.UnsupportedExportFormatException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class FileExportFormatTest {
    @Test
    void shouldResolveFormat_whenInputFormatIsProvidedIgnoringCase() {
        assertThat(ExportFormat.from("json")).isEqualTo(ExportFormat.JSON);
        assertThat(ExportFormat.from("XML")).isEqualTo(ExportFormat.XML);
    }

    @Test
    void shouldThrowUnsupportedExportFormatException_whenFormatIsUnknown() {
        assertThatThrownBy(() -> ExportFormat.from("yaml"))
                .isInstanceOf(UnsupportedExportFormatException.class);
    }

    @Test
    void shouldThrowUnsupportedExportFormatException_whenFormatIsMissingOrNull() {
        assertThatThrownBy(() -> ExportFormat.from(null))
                .isInstanceOf(UnsupportedExportFormatException.class);
    }
}
