package com.org.devgenie.controller.coverage;

import com.org.devgenie.dto.coverage.*;
import com.org.devgenie.service.coverage.CoverageAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coverage")
@Slf4j
public class CoverageController {

    @Autowired
    private CoverageAgentService coverageAgentService;

    @PostMapping("/increase-file")
    public ResponseEntity<CoverageResponse> increaseFileCoverage(@RequestBody FileCoverageRequest request) {
        try {
            CoverageResponse response = coverageAgentService.increaseFileCoverage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error increasing file coverage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CoverageResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/increase-repo")
    public ResponseEntity<CoverageResponse> increaseRepoCoverage(@RequestBody RepoCoverageRequest request) {
        try {
            CoverageResponse response = coverageAgentService.increaseRepoCoverage(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error increasing repo coverage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CoverageResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/apply-changes")
    public ResponseEntity<ApplyChangesResponse> applyChanges(@RequestBody ApplyChangesRequest request) {
        try {
            ApplyChangesResponse response = coverageAgentService.applyChanges(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error applying changes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApplyChangesResponse.error(e.getMessage()));
        }
    }
}
