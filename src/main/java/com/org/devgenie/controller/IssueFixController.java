package com.org.devgenie.controller;

import com.org.devgenie.model.ClassDescription;
import com.org.devgenie.service.IssueFixService;
import com.org.devgenie.util.LoggerUtil;
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

    private static final Logger logger = LoggerFactory.getLogger(IssueFixController.class);

    private final IssueFixService fixService;

    public IssueFixController(IssueFixService fixService) {
        this.fixService = fixService;
    }

    @PostMapping("/apply-fix")
    public ResponseEntity<Map<String, Object>> startFix(@RequestBody List<ClassDescription> classDescriptions) {
        try {
            String operationId = UUID.randomUUID().toString();
            fixService.startFix(operationId, classDescriptions);
            return ResponseEntity.ok(Map.of("success", true, "operationId", operationId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/fix-status/{operationId}")
    public ResponseEntity<Map<String, Object>> getFixStatus(@PathVariable String operationId) {
        logger.info("Operation progress status check ...{}", LoggerUtil.maskSensitive(operationId));
        List<String> step = fixService.getStatus(operationId);
        return ResponseEntity.ok(Map.of("step", step));
    }
}