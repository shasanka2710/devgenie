package com.org.javadoc.ai.generator.controller;

import com.org.javadoc.ai.generator.model.ClassDescription;
import com.org.javadoc.ai.generator.service.IssueFixService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/sonar/issue")
public class IssueFixController {

    private final IssueFixService fixService;
    private final static Logger logger = LoggerFactory.getLogger(IssueFixController.class);

    public IssueFixController(IssueFixService fixService) {
        this.fixService = fixService;
    }

    @PostMapping("/apply-fix")
    public ResponseEntity<?> startFix(@RequestBody List<ClassDescription> classDescriptions) {
        try {
            String operationId = UUID.randomUUID().toString();
            fixService.startFix(operationId, classDescriptions);
            return ResponseEntity.ok(Map.of("success", true, "operationId", operationId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/fix-status/{operationId}")
    public ResponseEntity<?> getFixStatus(@PathVariable String operationId) {
        logger.info("Operation progress status check ...{}", operationId);
        List<String> step = fixService.getStatus(operationId);
        return ResponseEntity.ok(Map.of("step", step));
    }
}
