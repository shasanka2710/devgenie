package com.org.devgenie.controller.coverage;

import com.org.devgenie.dto.coverage.*;
import com.org.devgenie.model.coverage.RepositoryAnalysisRequest;
import com.org.devgenie.model.coverage.RepositoryAnalysisResponse;
import com.org.devgenie.model.coverage.WorkspaceStatusResponse;
import com.org.devgenie.service.coverage.CoverageAgentService;
import com.org.devgenie.service.coverage.RepositoryAnalysisService;
import com.org.devgenie.service.coverage.RepositoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coverage")
@Slf4j
public class CoverageController {

    @Autowired
    private CoverageAgentService coverageAgentService;

    @Autowired
    private RepositoryAnalysisService repositoryAnalysisService;

    @Autowired
    private RepositoryService repositoryService;

    /**
     * NEW: Analyze repository and provide summary before coverage improvement
     * Sample Request:
     * {
     *   "repositoryUrl": "https://github.com/user/repo",
     *   "branch": "main",
     *   "githubToken": "ghp_xxxx" (optional for public repos)
     * }
     */
    @PostMapping("/analyze-repository")
    public ResponseEntity<RepositoryAnalysisResponse> analyzeRepository(@RequestBody RepositoryAnalysisRequest request) {
        try {
            RepositoryAnalysisResponse response = repositoryAnalysisService.analyzeRepository(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error analyzing repository", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RepositoryAnalysisResponse.error(e.getMessage()));
        }
    }

    /**
     * Enhanced file coverage improvement with repository context
     * Sample Request:
     * {
     *   "repositoryUrl": "https://github.com/user/repo",
     *   "branch": "main",
     *   "filePath": "src/main/java/com/example/service/UserService.java",
     *   "targetCoverageIncrease": 25.0,
     *   "githubToken": "ghp_xxxx",
     *   "workspaceId": "user-workspace-123"
     * }
     */
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

    /**
     * Enhanced repository coverage improvement with auto-detection
     * Sample Request:
     * {
     *   "repositoryUrl": "https://github.com/user/repo",
     *   "branch": "main",
     *   "targetCoverageIncrease": 20.0,
     *   "githubToken": "ghp_xxxx",
     *   "workspaceId": "user-workspace-123",
     *   "excludePatterns": ["test/**", "generated/**"],
     *   "maxFilesToProcess": 10,
     *   "forceProjectDetection": false
     * }
     */
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

    /**
     * Get workspace status and cached repository information
     */
    @GetMapping("/workspace/{workspaceId}/status")
    public ResponseEntity<WorkspaceStatusResponse> getWorkspaceStatus(@PathVariable String workspaceId) {
        try {
            WorkspaceStatusResponse response = repositoryService.getWorkspaceStatus(workspaceId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting workspace status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WorkspaceStatusResponse.error(e.getMessage()));
        }
    }

    /**
     * Clean up workspace (remove cloned repository)
     */
    @DeleteMapping("/workspace/{workspaceId}")
    public ResponseEntity<Void> cleanupWorkspace(@PathVariable String workspaceId) {
        try {
            repositoryService.cleanupWorkspace(workspaceId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error cleaning up workspace", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
