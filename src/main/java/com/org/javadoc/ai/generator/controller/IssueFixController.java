package com.org.javadoc.ai.generator.controller;

import com.org.javadoc.ai.generator.service.IssueFixService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sonar/issue")
public class IssueFixController {
    private static final Logger logger = LoggerFactory.getLogger(IssueFixController.class);

    @Autowired
    private IssueFixService fixService;

    @PostMapping("/apply-fix/{className}/desc/{description}")
    public ResponseEntity<?> startFix(@PathVariable String className, @PathVariable String description) {
        String operationId = fixService.startFix(className,description);
        return ResponseEntity.ok(Map.of("success", true, "operationId", operationId));
    }

    @GetMapping("/fix-status/{operationId}")
    public ResponseEntity<?> getFixStatus(@PathVariable String operationId) {
        String step = fixService.getStatus(operationId);
        String githubUrl = fixService.getPullRequestUrl(operationId);
        return ResponseEntity.ok(Map.of("step", step, "githubUrl", githubUrl));
    }
}
