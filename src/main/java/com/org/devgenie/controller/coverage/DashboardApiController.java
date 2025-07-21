package com.org.devgenie.controller.coverage;

import com.org.devgenie.service.coverage.RepositoryDashboardService;
import com.org.devgenie.service.coverage.FastDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@Slf4j
public class DashboardApiController {

    @Autowired
    private RepositoryDashboardService dashboardService;

    @Autowired
    private FastDashboardService fastDashboardService;

    @GetMapping("/coverage")
    public ResponseEntity<?> getCoverageData(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch) {
        try {
            log.info("Fetching FAST dashboard data for repo: {} branch: {}", repoPath, branch);

            RepositoryDashboardService.DashboardData dashboardData =
                    fastDashboardService.getFastDashboardData(repoPath, branch);

            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            log.error("Error fetching fast dashboard data for repo: {}", repoPath, e);
            return ResponseEntity.status(500).body("Error fetching dashboard data: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Dashboard API is healthy");
    }    @GetMapping("/cache/status")
    public ResponseEntity<?> getCacheStatus(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch) {
        try {
            // Check if cache exists and its status - we'll implement this method
            boolean cacheExists = fastDashboardService.isCacheAvailable(repoPath, branch);
            
            return ResponseEntity.ok(Map.of(
                "cacheExists", cacheExists,
                "repoPath", repoPath,
                "branch", branch
            ));
        } catch (Exception e) {
            log.error("Error checking cache status", e);
            return ResponseEntity.status(500).body("Error checking cache status: " + e.getMessage());
        }
    }

    @PostMapping("/cache/generate")
    public ResponseEntity<?> generateCache(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch) {
        try {
            log.info("Manually generating dashboard cache for repo: {}", repoPath);
            fastDashboardService.generateDashboardCacheAsync(repoPath, branch);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Dashboard cache generation started",
                    "repoPath", repoPath,
                    "branch", branch
            ));
        } catch (Exception e) {
            log.error("Error generating cache", e);
            return ResponseEntity.status(500).body("Error generating cache: " + e.getMessage());
        }
    }

    @DeleteMapping("/cache/clear")
    public ResponseEntity<?> clearCache(
            @RequestParam String repoPath,
            @RequestParam(defaultValue = "main") String branch) {
        try {
            log.info("Clearing dashboard cache for repo: {}", repoPath);
            fastDashboardService.clearDashboardCache(repoPath, branch);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Dashboard cache cleared",
                    "repoPath", repoPath,
                    "branch", branch
            ));
        } catch (Exception e) {
            log.error("Error clearing cache", e);
            return ResponseEntity.status(500).body("Error clearing cache: " + e.getMessage());
        }
    }
}
