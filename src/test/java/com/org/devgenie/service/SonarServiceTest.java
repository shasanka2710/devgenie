package com.org.devgenie.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SonarServiceTest {
    @Mock private RestTemplate restTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private MongoTemplate mongoTemplate;
    private SonarService sonarService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sonarService = new SonarService(
            restTemplate,
            objectMapper,
            "http://sonar.url",
            "componentKey",
            "CRITICAL",
            10,
            "user",
            "pass",
            mongoTemplate
        );
    }

    @Test
    void testGetSonarMetrics_noException() throws Exception {
        ObjectMapper realMapper = new ObjectMapper();
        String json = "{" +
                "\"total\": 5, \"effortTotal\": \"120\"}";
        when(objectMapper.readTree(anyString())).thenReturn(realMapper.readTree(json));
        sonarService.getSonarMetrics();
        // No exception = pass
    }

    @Test
    void testFetchIssuesFromSonar_handlesApiFailure() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class))).thenThrow(new RuntimeException("fail"));
        String result = sonarService.fetchIssuesFromSonar();
        assertEquals("{}", result);
    }

    @Test
    void testFetchSonarIssues_handlesInvalidJson() throws Exception {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
            .thenReturn(ResponseEntity.ok("not-json"));
        // Use a real ObjectMapper to ensure an exception is thrown for invalid JSON
        SonarService realMapperService = new SonarService(
            restTemplate,
            new ObjectMapper(),
            "http://sonar.url",
            "componentKey",
            "CRITICAL",
            10,
            "user",
            "pass",
            mongoTemplate
        );
        assertThrows(JsonProcessingException.class, () -> realMapperService.fetchSonarIssues());
    }
}
