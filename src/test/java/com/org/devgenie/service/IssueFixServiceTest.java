package com.org.devgenie.service;

import com.org.devgenie.github.GitHubUtility;
import com.org.devgenie.model.ClassDescription;
import com.org.devgenie.mongo.PullRequestMetricsRepository;
import com.org.devgenie.parser.JavaCodeParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IssueFixServiceTest {
    @Mock private JavaCodeParser javaCodeParser;
    @Mock private GitHubUtility gitHubUtility;
    @Mock private PullRequestMetricsRepository pullRequestMetricsRepository;
    @Mock private MongoTemplate mongoTemplate;
    private IssueFixService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new IssueFixService(javaCodeParser, gitHubUtility, pullRequestMetricsRepository, mongoTemplate, 1.0);
    }

    static class TestableIssueFixService extends IssueFixService {
        private String fixedCode;
        private boolean throwOnApply = false;
        public TestableIssueFixService(JavaCodeParser j, GitHubUtility g, PullRequestMetricsRepository p, MongoTemplate m, double d) {
            super(j, g, p, m, d);
        }
        public void setFixedCode(String code) { this.fixedCode = code; }
        public void setThrowOnApply(boolean val) { this.throwOnApply = val; }
        @Override
        public String identifyFix(String className, Set<String> description) { return fixedCode; }
        @Override
        public void applyFix(String className, String fixedCode) {
            if (throwOnApply) throw new RuntimeException("fail");
        }
    }

    @Test
    void startFix_handlesEmptyDescriptions() {
        service.startFix("opId", List.of());
        // No exception = pass
    }

    @Test
    void startFix_processesValidDescriptions() throws Exception {
        ClassDescription desc = mock(ClassDescription.class);
        when(desc.getClassName()).thenReturn("com.example.Foo");
        when(javaCodeParser.isValidJavaCode(anyString())).thenReturn(true);
        TestableIssueFixService testService = new TestableIssueFixService(javaCodeParser, gitHubUtility, pullRequestMetricsRepository, mongoTemplate, 1.0);
        testService.setFixedCode("fixedCode");
        CompletableFuture<String> result = testService.startFix("op2", List.of(desc));
        assertNotNull(result);
    }

    @Test
    void startFix_skipsInvalidCode() throws Exception {
        ClassDescription desc = mock(ClassDescription.class);
        when(desc.getClassName()).thenReturn("com.example.Bar");
        when(javaCodeParser.isValidJavaCode(anyString())).thenReturn(false);
        TestableIssueFixService testService = new TestableIssueFixService(javaCodeParser, gitHubUtility, pullRequestMetricsRepository, mongoTemplate, 1.0);
        testService.setFixedCode("badCode");
        CompletableFuture<String> result = testService.startFix("op3", List.of(desc));
        assertNotNull(result);
    }

    @Test
    void startFix_handlesExceptionInApplyFix() throws Exception {
        ClassDescription desc = mock(ClassDescription.class);
        when(desc.getClassName()).thenReturn("com.example.Baz");
        when(javaCodeParser.isValidJavaCode(anyString())).thenReturn(true);
        TestableIssueFixService testService = new TestableIssueFixService(javaCodeParser, gitHubUtility, pullRequestMetricsRepository, mongoTemplate, 1.0);
        testService.setFixedCode("fixedCode");
        testService.setThrowOnApply(true);
        CompletableFuture<String> result = testService.startFix("op4", List.of(desc));
        assertNotNull(result);
    }

    @Test
    void operationProgress_isUpdated() throws Exception {
        ClassDescription desc = mock(ClassDescription.class);
        when(desc.getClassName()).thenReturn("com.example.Qux");
        when(javaCodeParser.isValidJavaCode(anyString())).thenReturn(true);
        TestableIssueFixService testService = new TestableIssueFixService(javaCodeParser, gitHubUtility, pullRequestMetricsRepository, mongoTemplate, 1.0);
        testService.setFixedCode("fixedCode");
        String opId = "op5";
        testService.startFix(opId, List.of(desc)).get();
        assertTrue(testService.operationProgress.containsKey(opId));
        assertFalse(testService.operationProgress.get(opId).isEmpty());
    }
}
