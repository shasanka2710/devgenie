package com.org.devgenie.controller.coverage;

import com.org.devgenie.service.coverage.TestDataService;
import com.org.devgenie.service.coverage.FastDashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/test")
@Slf4j
public class TestDataController {

    @Autowired
    private TestDataService testDataService;

    @Autowired
    private FastDashboardService fastDashboardService;

    @GetMapping("/create-coverage-data")
    @ResponseBody
    public String createTestData() {
        try {
            // Create test coverage data
            testDataService.createTestCoverageData();
            
            // Generate dashboard cache for fast loading
            String testRepoUrl = "https://github.com/shasanka2710/devgenie";
            fastDashboardService.generateDashboardCacheSync(testRepoUrl, "main");
            
            return "Test coverage data and dashboard cache created successfully!";
        } catch (Exception e) {
            log.error("Error creating test data", e);
            return "Error creating test data: " + e.getMessage();
        }
    }
}
