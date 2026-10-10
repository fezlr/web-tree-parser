package com.fezlr.web_tree_parser.parsing.importer.service;

import com.fezlr.web_tree_parser.parsing.constant.ParsingLimits;
import com.fezlr.web_tree_parser.parsing.importer.exception.InvalidImportFileException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class FileImportServiceTest {
    private final FileImportService service = new FileImportService();

    @Test
    void shouldReadText_whenFileIsUtf8() {
        assertThat(service.read(text("{1, {a}}"))).isEqualTo("{1, {a}}");
    }

    @Test
    void shouldReadText_whenContentIsNonLatin() {
        assertThat(service.read(text("{\"привет\", Ω}"))).isEqualTo("{\"привет\", Ω}");
    }

    @Test
    void shouldKeepLineBreaks_whenFileHasWindowsLineBreaks() {
        assertThat(service.read(text("{\r\n  1,\r\n  2\r\n}"))).isEqualTo("{\r\n  1,\r\n  2\r\n}");
    }

    @Test
    void shouldStripByteOrderMark_whenBomIsPresent() {
        assertThat(service.read(text("\uFEFF{1}"))).isEqualTo("{1}");
    }

    @Test
    void shouldAcceptFile_whenExtensionIsUpperCase() {
        MockMultipartFile upper = file("SET.TXT", "{1}".getBytes(StandardCharsets.UTF_8));

        assertThat(service.read(upper)).isEqualTo("{1}");
    }

    @Test
    void shouldThrowInvalidImportFileException_whenFileIsMissing() {
        assertThatThrownBy(() -> service.read(null))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessage("Please choose a file");
    }

    @Test
    void shouldThrowInvalidImportFileException_whenFileIsEmpty() {
        assertThatThrownBy(() -> service.read(file("set.txt", new byte[0])))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessage("Please choose a file");
    }

    @Test
    void shouldThrowInvalidImportFileException_whenExtensionIsUnsupported() {
        assertThatThrownBy(() -> service.read(file("set.csv", "{1}".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessage("Only .txt files are supported");
    }

    @Test
    void shouldThrowInvalidImportFileException_whenFileHasNoName() {
        assertThatThrownBy(() -> service.read(file("", "{1}".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessage("Only .txt files are supported");
    }

    @Test
    void shouldThrowInvalidImportFileException_whenContentIsNotUtf8() {
        byte[] windows1251 = {(byte) 0xCF, (byte) 0xF0, (byte) 0xE8};

        assertThatThrownBy(() -> service.read(file("set.txt", windows1251)))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessage("The file must be UTF-8 text");
    }

    @Test
    void shouldThrowInvalidImportFileException_whenFileIsOversized() {
        byte[] content = new byte[ParsingLimits.MAX_FILE_BYTES + 1];
        Arrays.fill(content, (byte) 'a');

        assertThatThrownBy(() -> service.read(file("set.txt", content)))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessageContaining("390.625 KB");
    }

    @Test
    void shouldThrowInvalidImportFileException_whenContentHasTooManyCharacters() {
        String content = "{" + "a".repeat(ParsingLimits.MAX_INPUT_LENGTH) + "}";

        assertThatThrownBy(() -> service.read(text(content)))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessageContaining(String.valueOf(ParsingLimits.MAX_INPUT_LENGTH));
    }

    @Test
    void shouldThrowInvalidImportFileException_whenContentIsBlank() {
        assertThatThrownBy(() -> service.read(text("  \r\n ")))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessage("The file is empty");
    }

    @Test
    void shouldThrowInvalidImportFileException_whenContentContainsOnlyByteOrderMark() {
        assertThatThrownBy(() -> service.read(text("\uFEFF")))
                .isInstanceOf(InvalidImportFileException.class)
                .hasMessage("The file is empty");
    }

    private MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "text/plain", content);
    }

    private MockMultipartFile text(String content) {
        return file("set.txt", content.getBytes(StandardCharsets.UTF_8));
    }
}
