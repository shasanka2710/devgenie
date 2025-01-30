package com.org.javadoc.ai.generator.controller;

import com.org.javadoc.ai.generator.service.IssueFixService;
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

    public IssueFixController(IssueFixService fixService) {
        this.fixService = fixService;
    }

    @PostMapping("/apply-fix")
    public ResponseEntity<?> startFix(@RequestBody List<Map<String, String>> classDescriptions) {
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
        List<String> step = fixService.getStatus(operationId);
        return ResponseEntity.ok(Map.of("step", step));
    }
}
