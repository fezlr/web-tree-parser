package com.fezlr.web_tree_parser.parsing.importer.service;

import com.fezlr.web_tree_parser.parsing.constant.ParsingLimits;
import com.fezlr.web_tree_parser.parsing.importer.exception.InvalidImportFileException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class FileImportService {
    private static final String ALLOWED_EXTENSION = ".txt";
    private static final char BYTE_ORDER_MARK = '\uFEFF';

    public String read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImportFileException("Please choose a file");
        }

        String name = file.getOriginalFilename();

        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(ALLOWED_EXTENSION)) {
            throw new InvalidImportFileException("Only .txt files are supported");
        }

        if (file.getSize() > ParsingLimits.MAX_FILE_BYTES) {
            throw new InvalidImportFileException(
                    "File must not exceed " + ParsingLimits.MAX_FILE_BYTES / 1024.0 + " KB");
        }

        String text = decode(file);

        if (!text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK) {
            text = text.substring(1);
        }

        if (text.isBlank()) {
            throw new InvalidImportFileException("The file is empty");
        }

        if (text.length() > ParsingLimits.MAX_INPUT_LENGTH) {
            throw new InvalidImportFileException(
                    "File content must not exceed " + ParsingLimits.MAX_INPUT_LENGTH + " characters");
        }

        return text;
    }

    private String decode(MultipartFile file) {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);

            return decoder.decode(ByteBuffer.wrap(file.getBytes())).toString();
        } catch (CharacterCodingException e) {
            throw new InvalidImportFileException("The file must be UTF-8 text");
        } catch (IOException e) {
            throw new InvalidImportFileException("The file could not be read");
        }
    }
}
