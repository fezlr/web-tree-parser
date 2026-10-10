package com.fezlr.web_tree_parser.parsing.exporter.service;

import com.fezlr.web_tree_parser.TestcontainersConfiguration;
import com.fezlr.web_tree_parser.core.lexer.Lexer;
import com.fezlr.web_tree_parser.core.node.Node;
import com.fezlr.web_tree_parser.core.parser.Parser;
import com.fezlr.web_tree_parser.core.stats.StatsAssemble;
import com.fezlr.web_tree_parser.history.exception.ParseHistoryNotFoundException;
import com.fezlr.web_tree_parser.history.mapper.ParseHistoryMapper;
import com.fezlr.web_tree_parser.history.repository.ParseHistoryRepository;
import com.fezlr.web_tree_parser.history.service.ParseHistoryService;
import com.fezlr.web_tree_parser.parsing.exporter.config.JacksonConfig;
import com.fezlr.web_tree_parser.parsing.exporter.dto.ExportFormat;
import com.fezlr.web_tree_parser.parsing.exporter.dto.request.ExportFileRequest;
import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.service.UserService;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.w3c.dom.Document;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, JacksonConfig.class})
public class FileExportServiceTest {
    private static final String SOURCE = "{1, {a, \"x\"}}";
    @Autowired
    private FileExportService fileExportService;

    @Autowired
    private ParseHistoryRepository parseHistoryRepository;

    @Autowired
    private ParseHistoryService parseHistoryService;

    @Autowired
    private ParseHistoryMapper parseHistoryMapper;

    @Autowired
    private UserService userService;

    @Test
    void shouldExportJsonWithMetadataTreeAndStatistics_whenFormatIsJson() throws JsonProcessingException {
        UserEntity userEntity = newUser("exportjsonowner", Role.USER);
        Long id = save(userEntity, SOURCE);

        DocumentContext document = json(fileExportService.export(id, userEntity, ExportFormat.JSON));

        assertThat(document.<Number>read("$.id").longValue()).isEqualTo(id);
        assertThat(document.<String>read("$.author")).isEqualTo("exportjsonowner");
        assertThat(document.<String>read("$.source")).isEqualTo(SOURCE);
        assertThat(document.<String>read("$.createdAt")).isNotBlank();
        assertThat(document.<String>read("$.tree.type")).isEqualTo("set");
        assertThat(document.<String>read("$.tree.elements[0].type")).isEqualTo("number");
        assertThat(document.<String>read("$.tree.elements[0].value")).isEqualTo("1");
        assertThat(document.<String>read("$.tree.elements[1].type")).isEqualTo("set");
        assertThat(document.<String>read("$.tree.elements[1].elements[0].type")).isEqualTo("identifier");
        assertThat(document.<String>read("$.tree.elements[1].elements[1].type")).isEqualTo("string");
        assertThat(document.<String>read("$.tree.elements[1].elements[1].value")).isEqualTo("x");
        assertThat(document.<Integer>read("$.statistics.totalElements")).isEqualTo(3);
        assertThat(document.<Integer>read("$.statistics.subsets")).isEqualTo(1);
        assertThat(document.<Integer>read("$.statistics.maxDepth")).isEqualTo(2);
    }

    @Test
    void shouldOmitFieldsThatDoNotApplyToNodeKind_whenJsonIsGenerated() throws JsonProcessingException {
        UserEntity owner = newUser("exportjsonshape", Role.USER);
        Long id = save(owner, "{1}");

        DocumentContext document = json(fileExportService.export(id, owner, ExportFormat.JSON));

        assertThat(document.<Map<String, Object>>read("$.tree")).doesNotContainKey("value");
        assertThat(document.<Map<String, Object>>read("$.tree.elements[0]")).doesNotContainKey("elements");
    }

    @Test
    void shouldKeepEmptySetAsEmptyArray_whenSetIsEmpty() throws JsonProcessingException {
        UserEntity owner = newUser("exportjsonempty", Role.USER);
        Long id = save(owner, "{}");

        DocumentContext document = json(fileExportService.export(id, owner, ExportFormat.JSON));

        assertThat(document.<java.util.List<Object>>read("$.tree.elements")).isEmpty();
    }

    @Test
    void shouldExportXmlWithMetadataTreeAndStatistics_whenFormatIsXml() throws Exception {
        UserEntity userEntity = newUser("exportxmlowner", Role.USER);
        Long id = save(userEntity, SOURCE);

        Document document = xml(fileExportService.export(id, userEntity, ExportFormat.XML));

        assertThat(xpath(document, "/parseResult/@id")).isEqualTo(String.valueOf(id));
        assertThat(xpath(document, "/parseResult/@author")).isEqualTo("exportxmlowner");
        assertThat(xpath(document, "/parseResult/source")).isEqualTo(SOURCE);
        assertThat(xpath(document, "/parseResult/tree/set/number")).isEqualTo("1");
        assertThat(xpath(document, "/parseResult/tree/set/set/identifier")).isEqualTo("a");
        assertThat(xpath(document, "/parseResult/tree/set/set/string")).isEqualTo("x");
        assertThat(xpath(document, "/parseResult/statistics/@totalElements")).isEqualTo("3");
        assertThat(xpath(document, "/parseResult/statistics/@maxDepth")).isEqualTo("2");
    }

    @Test
    void shouldStayWellFormedForControlCharactersInStrings_whenXmlIsExported() throws Exception {
        UserEntity userEntity = newUser("exportxmlcontrol", Role.USER);
        Long id = save(userEntity, "{\"a\u0001b\"}");

        Document document = xml(fileExportService.export(id, userEntity, ExportFormat.XML));

        assertThat(xpath(document, "/parseResult/tree/set/string")).isEqualTo("a\uFFFDb");
    }

    @Test
    void shouldKeepControlCharactersInStrings_whenJsonIsExported() throws JsonProcessingException {
        UserEntity userEntity = newUser("exportjsoncontrol", Role.USER);
        Long id = save(userEntity, "{\"a\u0001b\"}");

        DocumentContext document = json(fileExportService.export(id, userEntity, ExportFormat.JSON));

        assertThat(document.<String>read("$.tree.elements[0].value")).isEqualTo("a\u0001b");
    }

    @Test
    void shouldBuildFileNameAndContentTypeFromFormat_whenExportIsExecuted() throws JsonProcessingException {
        UserEntity owner = newUser("exportfilename", Role.USER);
        Long id = save(owner, "{1}");

        ExportFileRequest jsonFile = fileExportService.export(id, owner, ExportFormat.JSON);
        ExportFileRequest xmlFile = fileExportService.export(id, owner, ExportFormat.XML);

        assertThat(jsonFile.fileName()).isEqualTo("parse-result-" + id + ".json");
        assertThat(jsonFile.contentType()).isEqualTo("application/json");
        assertThat(xmlFile.fileName()).isEqualTo("parse-result-" + id + ".xml");
        assertThat(xmlFile.contentType()).isEqualTo("application/xml");
    }

    @Test
    void shouldHideEntryFromOtherUsers_whenUserIsNotOwner() {
        UserEntity owner = newUser("exportprivateowner", Role.USER);
        UserEntity stranger = newUser("exportprivatestranger", Role.USER);
        Long id = save(owner, "{1}");

        assertThatThrownBy(() -> fileExportService.export(id, stranger, ExportFormat.JSON))
                .isInstanceOf(ParseHistoryNotFoundException.class);
        assertThatThrownBy(() -> fileExportService.export(id, stranger, ExportFormat.XML))
                .isInstanceOf(ParseHistoryNotFoundException.class);
    }

    @Test
    void shouldLetAdministratorExportAnyEntry_whenRequestedByAdmin() throws JsonProcessingException {
        UserEntity owner = newUser("exportadminowner", Role.USER);
        UserEntity admin = newUser("exportadminviewer", Role.ADMIN);
        Long id = save(owner, "{1}");

        DocumentContext document = json(fileExportService.export(id, admin, ExportFormat.JSON));

        assertThat(document.<String>read("$.author")).isEqualTo("exportadminowner");
    }

    private UserEntity newUser(String username, Role role) {
        return userService.create(username, "password123", role);
    }

    private Long save(UserEntity userEntity, String source) {
        Node tree = new Parser(new Lexer(source).tokenize()).parse();
        return parseHistoryService.save(parseHistoryMapper.toEntity(userEntity, source, new StatsAssemble().assemble(tree))).getId();
    }

    private DocumentContext json(ExportFileRequest file) {
        return JsonPath.parse(new String(file.content(), StandardCharsets.UTF_8));
    }

    private Document xml(ExportFileRequest file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(file.content()));
    }

    private String xpath(Document document, String expression) throws Exception {
        return XPathFactory.newInstance().newXPath().evaluate(expression, document);
    }
}
