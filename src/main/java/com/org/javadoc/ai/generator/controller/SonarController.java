package com.org.javadoc.ai.generator.controller;

import com.org.javadoc.ai.generator.model.SonarIssue;
import com.org.javadoc.ai.generator.service.SonarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/sonar")
public class SonarController {

    @Autowired
    private SonarService sonarService;

    @GetMapping("/issues")
    public String getIssues(
            @RequestParam(value = "filterType", required = false) String filterType,
            Model model) {
        log.info("Fetching issues with filterType: {}", filterType);

        try {
            List<SonarIssue> issues = sonarService.fetchSonarIssues();
            List<String> issueTypes = getDistinctIssueTypes(issues);

            if (filterType != null && !filterType.isEmpty()) {
                issues = issues.stream()
                        .filter(issue -> filterType.equals(issue.getType()))
                        .collect(Collectors.toList());
            }

            model.addAttribute("issues", issues);
            model.addAttribute("issueTypes", issueTypes);
            model.addAttribute("selectedType", filterType);
            return "insights";
        } catch (IOException e) {
            log.error("Error parsing issues: {}", e.getMessage());
            return "error";
        }
    }

    private List<String> getDistinctIssueTypes(List<SonarIssue> issues) {
        return issues.stream()
                .map(SonarIssue::getType)
                .distinct()
                .collect(Collectors.toList());
    }
}