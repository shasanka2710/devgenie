package com.org.devgenie.controller;

import com.org.devgenie.service.IssueFixService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class IssueFixControllerTest {
    @Mock
    private IssueFixService fixService;
    @InjectMocks
    private IssueFixController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void startFix_success() {
        when(fixService.startFix(anyString(), anyList())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture("id"));
        ResponseEntity<Map<String, Object>> resp = controller.startFix(List.of());
        assert (Boolean) resp.getBody().get("success");
        assert resp.getBody().get("operationId") != null;
    }

    @Test
    void startFix_failure() {
        doThrow(new RuntimeException("fail")).when(fixService).startFix(anyString(), anyList());
        ResponseEntity<Map<String, Object>> resp = controller.startFix(List.of());
        assert !(Boolean) resp.getBody().get("success");
        assert resp.getBody().get("message").equals("fail");
    }

    @Test
    void getFixStatus_success() {
        when(fixService.getStatus(anyString())).thenReturn(List.of("step1"));
        ResponseEntity<Map<String, Object>> resp = controller.getFixStatus("id");
        assert ((List<?>) resp.getBody().get("step")).get(0).equals("step1");
    }
}
