package com.org.javadoc.ai.generator.controller;

import com.org.javadoc.ai.generator.model.SonarIssue;
import com.org.javadoc.ai.generator.service.SonarQubeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/insights-metrics")
public class InsightsController {
    private final SonarQubeService sonarQubeService;

    public InsightsController(SonarQubeService sonarQubeService) {
        this.sonarQubeService = sonarQubeService;
    }

    @GetMapping
    public String getInsightsPage(Model model) {
        // Fetch issues from SonarQube
        List<SonarIssue> issues = sonarQubeService.fetchIssues("your-project-key");
        model.addAttribute("issues", issues);
        return "insights-metrics";
    }

    @PostMapping("/fix-issue/{issueId}")
    public ResponseEntity<String> fixIssue(@PathVariable String issueId) {
        try {
           // issueFixerService.fixIssue(issueId); // Service method to fix the issue
            return ResponseEntity.ok("Issue resolved successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to resolve the issue.");
        }
    }
}
