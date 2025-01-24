package com.org.javadoc.ai.generator.parser;

import com.org.javadoc.ai.generator.ai.SpringAiCommentGenerator;
import com.org.javadoc.ai.generator.ai.client.AiClient;
import com.org.javadoc.ai.generator.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.mockito.Mockito.when;

public class JavaCodeParserTest {

    @TempDir
    Path tempDir;

    @Mock
    private SpringAiCommentGenerator aiCommentGenerator;

    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private JavaCodeParser javaCodeParser;

    @Mock
    AiClient client;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

   // @Test
    void testParseAndGenerateDocs() throws IOException {
        // Create a temporary Java file
        Path javaFilePath = tempDir.resolve("TestClass.java");
        Files.write(javaFilePath, Collections.singletonList("public class TestClass { public void testMethod() {} }"));

        // Configure mocks
        when(appConfig.isDryRun()).thenReturn(false);
        when(appConfig.getIncludePaths()).thenReturn(Collections.emptyList());
        when(appConfig.getExcludePaths()).thenReturn(Collections.emptyList());
        when(appConfig.isEnableAi()).thenReturn(false);

        // Call the method
        javaCodeParser.parseAndGenerateDocs(javaFilePath.toFile());

        // Verify the file was modified
        String modifiedContent = Files.readString(javaFilePath);
        assert(modifiedContent.contains("TODO: Add class description here."));
        assert(modifiedContent.contains("TODO: Add method description here."));
    }

    @Test
    void testParseAndGenerateDocsFromFile() throws IOException {
        // Path to the existing Java file
        Path javaFilePath = Path.of("/Users/shasanka/development/POC/javadoc-generator-ai/src/main/java/com/org/javadoc/ai/generator/ai/SpringAiCommentGenerator.java");

        // Ensure the file exists
        if (!Files.exists(javaFilePath)) {
            throw new IOException("File not found: " + javaFilePath);
        }

        // Configure mocks
        when(appConfig.isDryRun()).thenReturn(false);
        when(appConfig.getIncludePaths()).thenReturn(Collections.emptyList());
        when(appConfig.getExcludePaths()).thenReturn(Collections.emptyList());
        when(appConfig.isEnableAi()).thenReturn(false);

        // Call the method
        javaCodeParser.parseAndGenerateDocs(javaFilePath.toFile());

        // Verify the file was modified
        String modifiedContent = Files.readString(javaFilePath);
        assert(modifiedContent.contains("TODO: Add class description here."));
        assert(modifiedContent.contains("TODO: Add method description here."));
    }
}
