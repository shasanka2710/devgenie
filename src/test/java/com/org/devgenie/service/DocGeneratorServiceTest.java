package com.org.devgenie.service;

import com.org.devgenie.config.AppConfig;
import com.org.devgenie.input.GitInputProcessor;
import com.org.devgenie.input.LocalFileInputProcessor;
import com.org.devgenie.parser.JavaCodeParser;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.io.IOException;
import static org.mockito.Mockito.*;

public class DocGeneratorServiceTest {
    @Mock private AppConfig appConfig;
    @Mock private JavaCodeParser javaCodeParser;
    @Mock private LocalFileInputProcessor localFileInputProcessor;
    @Mock private GitInputProcessor gitInputProcessor;
    @InjectMocks private DocGeneratorService service;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void generateDocs_invalidType_throws() {
        try {
            service.generateDocs("invalid", "src", null, null);
            assert false;
        } catch (IllegalArgumentException | IOException | GitAPIException e) {
            assert e.getMessage().contains("Invalid input type");
        }
    }
}
