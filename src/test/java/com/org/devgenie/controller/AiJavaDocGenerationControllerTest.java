package com.org.devgenie.controller;

import com.org.devgenie.exception.DocGenerationException;
import com.org.devgenie.service.DocGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class AiJavaDocGenerationControllerTest {
    @Mock
    private DocGeneratorService docGeneratorService;
    @InjectMocks
    private AiJavaDocGenerationController controller;

    @BeforeEach
    void setUp() { MockitoAnnotations.openMocks(this); }

    @Test
    void generateDocs_success() throws Exception {
        doNothing().when(docGeneratorService).generateDocs(anyString(), anyString(), any(), any());
        String result = controller.generateDocs("type", "src", null, null);
        assert result.contains("Documentation generated successfully");
    }

    @Test
    void generateDocs_failure() throws Exception {
        doThrow(new java.io.IOException("fail")).when(docGeneratorService).generateDocs(anyString(), anyString(), any(), any());
        try {
            controller.generateDocs("type", "src", null, null);
            assert false;
        } catch (DocGenerationException e) {
            System.out.println("Exception message: " + e.getMessage());
            assert e.getMessage() != null;
            assert e.getMessage().contains("Error generating documentation");
        }
    }
}
