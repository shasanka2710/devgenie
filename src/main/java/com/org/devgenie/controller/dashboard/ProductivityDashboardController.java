package com.org.devgenie.controller.dashboard;

import com.org.devgenie.dto.dashboard.DashboardFilterDto;
import com.org.devgenie.dto.dashboard.DashboardSummaryDto;
import com.org.devgenie.dto.dashboard.ImprovementRecordDto;
import com.org.devgenie.service.dashboard.ProductivityDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repository/{repositoryId}/dashboard")
@Slf4j
public class ProductivityDashboardController {

    @Autowired
    private ProductivityDashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(
            @PathVariable String repositoryId,
            @RequestParam String repositoryUrl,
            @RequestParam(defaultValue = "main") String branch,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "ALL") String subCategory,
            @RequestParam(defaultValue = "LAST_30_DAYS") String timeRange) {
        
        try {
            log.info("Fetching dashboard summary for repository: {} branch: {}", repositoryUrl, branch);

            DashboardFilterDto filter = DashboardFilterDto.builder()
                    .category(category)
                    .subCategory(subCategory)
                    .timeRange(timeRange)
                    .build();

            DashboardSummaryDto summary = dashboardService.getDashboardSummary(repositoryUrl, branch, filter);
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("Error fetching dashboard summary for repository: {}", repositoryUrl, e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/records")
    public ResponseEntity<Page<ImprovementRecordDto>> getImprovementRecords(
            @PathVariable String repositoryId,
            @RequestParam String repositoryUrl,
            @RequestParam(defaultValue = "main") String branch,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "ALL") String subCategory,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "LAST_30_DAYS") String timeRange,
            @RequestParam(defaultValue = "DATE_DESC") String sortBy,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        
        try {
            log.info("Fetching improvement records for repository: {} branch: {} page: {}", repositoryUrl, branch, page);

            DashboardFilterDto filter = DashboardFilterDto.builder()
                    .category(category)
                    .subCategory(subCategory)
                    .status(status)
                    .timeRange(timeRange)
                    .sortBy(sortBy)
                    .page(page)
                    .size(size)
                    .build();

            Page<ImprovementRecordDto> records = dashboardService.getImprovementRecords(repositoryUrl, branch, filter);
            return ResponseEntity.ok(records);

        } catch (Exception e) {
            log.error("Error fetching improvement records for repository: {}", repositoryUrl, e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/session/{sessionId}/modify")
    public ResponseEntity<String> modifySession(
            @PathVariable String repositoryId,
            @PathVariable String sessionId,
            @RequestBody String modifications) {
        
        try {
            log.info("Modifying session: {} for repository: {}", sessionId, repositoryId);
            
            // TODO: Implement session modification logic
            // This would involve:
            // 1. Validating session can be modified
            // 2. Applying modifications
            // 3. Triggering regeneration if needed
            
            return ResponseEntity.ok("Session modification functionality will be implemented in future iterations");

        } catch (Exception e) {
            log.error("Error modifying session: {} for repository: {}", sessionId, repositoryId, e);
            return ResponseEntity.status(500).body("Error modifying session: " + e.getMessage());
        }
    }

    @PostMapping("/session/{sessionId}/regenerate")
    public ResponseEntity<String> regenerateSession(
            @PathVariable String repositoryId,
            @PathVariable String sessionId,
            @RequestBody(required = false) String instructions) {
        
        try {
            log.info("Regenerating session: {} for repository: {} with instructions: {}", sessionId, repositoryId, instructions);
            
            // TODO: Implement session regeneration logic
            // This would involve:
            // 1. Validating session can be regenerated
            // 2. Applying new instructions
            // 3. Starting new improvement process
            
            return ResponseEntity.ok("Session regeneration functionality will be implemented in future iterations");

        } catch (Exception e) {
            log.error("Error regenerating session: {} for repository: {}", sessionId, repositoryId, e);
            return ResponseEntity.status(500).body("Error regenerating session: " + e.getMessage());
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(
            @PathVariable String repositoryId,
            @RequestParam String repositoryUrl,
            @RequestParam(defaultValue = "main") String branch,
            @RequestParam(defaultValue = "LAST_30_DAYS") String timeRange) {
        
        try {
            log.info("Fetching analytics for repository: {} branch: {}", repositoryUrl, branch);

            DashboardFilterDto filter = DashboardFilterDto.builder()
                    .timeRange(timeRange)
                    .build();

            DashboardSummaryDto summary = dashboardService.getDashboardSummary(repositoryUrl, branch, filter);
            return ResponseEntity.ok(summary.getAnalytics());

        } catch (Exception e) {
            log.error("Error fetching analytics for repository: {}", repositoryUrl, e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Productivity Dashboard API is healthy");
    }
}
