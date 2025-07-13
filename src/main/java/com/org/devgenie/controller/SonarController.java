package com.org.devgenie.controller;

import com.org.devgenie.model.SonarIssue;
import com.org.devgenie.service.SonarService;
import com.org.devgenie.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.IOException;
import java.util.List;
import static com.org.devgenie.util.StringUtil.getclassDisplayName;

@Controller
@RequestMapping("/sonar")
public class SonarController {
    private final Logger log = LoggerFactory.getLogger(SonarController.class);

    private final SonarService sonarService;

    public SonarController(SonarService sonarService) {
        this.sonarService = sonarService;
    }

    @GetMapping("/issues")
    public String getIssues(@RequestParam(value = "filterType", required = false) String filterType, Model model) {
        log.info("Fetching issues with filterType: {} and filterQuality: {}", LoggerUtil.maskSensitive(filterType), "MASKED");
        try {
            List<SonarIssue> issues = sonarService.fetchSonarIssues();
            List<String> severities = getDistinctSeverity(issues);

            if (filterType != null && !filterType.isEmpty()) {
                // Use Stream.toList() for better performance and conciseness
                issues = issues.stream().filter(issue -> issue.getSeverity().contains(filterType)).toList();
            }
            issues.forEach(issue -> issue.setClassName(getclassDisplayName(issue.getCategory())));
            model.addAttribute("issues", issues);
            model.addAttribute("severities", severities);
            model.addAttribute("selectedType", filterType);
            return "insights";
        } catch (IOException e) {
            log.error("Error parsing issues: {}", LoggerUtil.maskSensitive(e.getMessage()));
            return "error";
        }
    }

    private List<String> getDistinctSeverity(List<SonarIssue> issues) {
        // Use Stream.toList() for better performance and conciseness
        return issues.stream().map(SonarIssue::getSeverity).distinct().toList();
    }
}