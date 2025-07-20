package com.org.devgenie.controller.coverage;

import com.org.devgenie.mongo.CoverageDataFlatMongoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

@Controller
public class CoverageDashboardController {

    @Autowired
    private CoverageDataFlatMongoRepository coverageDataFlatMongoRepository;

    @Value("${sonar.componentKeys}")
    private String key;



 /*  @GetMapping("/repository-dashboard")
    public String getRepositoryDashboard(Model model) {
        // Fetch root directories from CoverageNodeMongoRepository
        List<DirectoryCoverageData> directories = coverageNodeMongoRepository.findAll().stream()
            .filter(node -> node instanceof DirectoryCoverageData)
            .map(node -> (DirectoryCoverageData) node)
            .filter(dir -> dir.getParentPath() == null || dir.getParentPath().isEmpty())
            .collect(Collectors.toList());
        model.addAttribute("directories", directories);
        return "repository-dashboard";
    }*/
}