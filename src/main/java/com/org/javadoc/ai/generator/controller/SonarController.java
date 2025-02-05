package com.org.javadoc.ai.generator.controller;

import com.org.javadoc.ai.generator.model.SonarIssue;
import com.org.javadoc.ai.generator.service.SonarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.IOException;
import java.util.List;
import static com.org.javadoc.ai.generator.util.StringUtil.getclassDisplayName;

@Slf4j
@Controller
@RequestMapping("/sonar")
public class SonarController {

    private final SonarService sonarService;

    public SonarController(SonarService sonarService) {
        this.sonarService = sonarService;
    }

    @GetMapping("/issues")
    public String getIssues(@RequestParam(value = "filterType", required = false) String filterType, Model model) {
        log.info("Fetching issues with filterType: {} and filterQuality: {}", filterType);
        try {
            List<SonarIssue> issues = sonarService.fetchSonarIssues();
            List<String> issueTypes = getDistinctIssueTypes(issues);
            List<String> softwareQualities = getDistinctSoftwareQualities(issues);
            if (filterType != null && !filterType.isEmpty()) {
                // Use Stream.toList() for better performance and conciseness
                issues = issues.stream().filter(issue -> issue.getSoftwareQuality().contains(filterType)).toList();
            }
            issues.forEach(issue -> issue.setClassName(getclassDisplayName(issue.getCategory())));
            model.addAttribute("issues", issues);
            model.addAttribute("issueTypes", issueTypes);
            model.addAttribute("softwareQualities", softwareQualities);
            model.addAttribute("selectedType", filterType);
            return "insights";
        } catch (IOException e) {
            log.error("Error parsing issues: {}", e.getMessage());
            return "error";
        }
    }

    private List<String> getDistinctSoftwareQualities(List<SonarIssue> issues) {
        // Use Stream.toList() for better performance and conciseness
        return issues.stream().flatMap(issue -> issue.getSoftwareQuality().stream()).distinct().toList();
    }

    private List<String> getDistinctIssueTypes(List<SonarIssue> issues) {
        // Use Stream.toList() for better performance and conciseness
        return issues.stream().map(SonarIssue::getType).distinct().toList();
    }
}