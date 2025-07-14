package com.org.devgenie.controller.coverage;

import com.org.devgenie.model.CoverageComponentNode;
import com.org.devgenie.service.CoverageTreeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class CoverageDashboardController {

    @Autowired
    private CoverageTreeService coverageTreeService;

    @Value("${sonar.componentKeys}")
    private String key;

    @GetMapping("/coverage-dashboard")
    public String getCoverageDashboard(@RequestParam(value = "parentPath", required = false) String parentPath, Model model) {
        List<CoverageComponentNode> nodes = coverageTreeService.fetchAndStoreComponentsWithCoverage(key);

        // Deduplicate by path
        List<CoverageComponentNode> uniqueRecords = nodes.stream()
                .collect(Collectors.toMap(CoverageComponentNode::getPath, Function.identity(), (a, b) -> a))
                .values()
                .stream()
                .collect(Collectors.toList());

        List<CoverageComponentNode> filtered;
        if (parentPath == null || parentPath.isEmpty()) {
            filtered = uniqueRecords.stream()
                    .filter(n -> n.getType().equals("DIR"))
                    .collect(Collectors.toList());
            model.addAttribute("currentPath", "src/main");
        } else {
            int parentDepth = parentPath.split("/").length;
            filtered = uniqueRecords.stream()
                    .filter(n -> n.getPath().startsWith(parentPath + "/") &&
                            n.getPath().split("/").length == parentDepth + 1)
                    .collect(Collectors.toList());
            model.addAttribute("currentPath", parentPath);
        }

        model.addAttribute("records", filtered);
        model.addAttribute("page","coverage");
        return "coverage-dashboard";
    }

    @GetMapping("/coverage-dashboard/children")
    @ResponseBody
    public List<CoverageComponentNode> getChildren(@RequestParam("parentPath") String parentPath) {
        List<CoverageComponentNode> nodes = coverageTreeService.getAllComponents();

        List<CoverageComponentNode> uniqueRecords = nodes.stream()
                .collect(Collectors.toMap(CoverageComponentNode::getPath, Function.identity(), (a, b) -> a))
                .values()
                .stream()
                .collect(Collectors.toList());

        int parentDepth = parentPath.split("/").length;
        return uniqueRecords.stream()
                .filter(n -> n.getPath().startsWith(parentPath + "/") &&
                        n.getPath().split("/").length == parentDepth + 1)
                .collect(Collectors.toList());
    }
}