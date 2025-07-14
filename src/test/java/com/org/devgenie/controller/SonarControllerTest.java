package com.org.devgenie.controller;

import com.org.devgenie.model.SonarIssue;
import com.org.devgenie.service.SonarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class SonarControllerTest {
    @Mock
    private SonarService sonarService;

    @InjectMocks
    private SonarController sonarController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getIssues_returnsInsights_whenNoFilter() throws IOException {
        List<SonarIssue> issues = Arrays.asList(
                new SonarIssue("CRITICAL", "cat1"),
                new SonarIssue("MAJOR", "cat2")
        );
        when(sonarService.fetchSonarIssues()).thenReturn(issues);
        Model model = new ConcurrentModel();
        String view = sonarController.getIssues(null, model);
        assert view.equals("insights");
        assert model.getAttribute("issues") != null;
        assert model.getAttribute("severities") != null;
    }

    @Test
    void getIssues_returnsInsights_withFilter() throws IOException {
        List<SonarIssue> issues = Arrays.asList(
                new SonarIssue("CRITICAL", "cat1"),
                new SonarIssue("MAJOR", "cat2")
        );
        when(sonarService.fetchSonarIssues()).thenReturn(issues);
        Model model = new ConcurrentModel();
        String view = sonarController.getIssues("CRITICAL", model);
        assert view.equals("insights");
        List<?> filtered = (List<?>) model.getAttribute("issues");
        assert filtered.size() == 1;
        assert ((SonarIssue)filtered.get(0)).getSeverity().equals("CRITICAL");
    }

    @Test
    void getIssues_returnsError_onIOException() throws IOException {
        when(sonarService.fetchSonarIssues()).thenThrow(new IOException("fail"));
        Model model = new ConcurrentModel();
        String view = sonarController.getIssues(null, model);
        assert view.equals("error");
    }
}
