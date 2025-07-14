package com.org.devgenie.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.Mockito.*;

public class CallGraphControllerTest {
    @Mock
    private com.org.devgenie.parser.JavaCodeParser javaCodeParser;
    @InjectMocks
    private CallGraphController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void index_returnsIndex() {
        assert controller.index().equals("index");
    }

    @Test
    void uploadFile_emptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        Model model = new ConcurrentModel();
        String view = controller.uploadFile(file, model);
        assert view.equals("index");
        assert model.getAttribute("message").equals("Please select a file to upload.");
    }

    @Test
    void uploadFile_nullFileName() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(null);
        Model model = new ConcurrentModel();
        String view = controller.uploadFile(file, model);
        assert view.equals("index");
        assert model.getAttribute("message").equals("File name is missing or invalid.");
    }

    // Note: For file IO and call graph, integration tests are more appropriate.
}
