package com.org.devgenie.service.coverage;

import com.org.devgenie.exception.coverage.CoverageDataNotFoundException;
import com.org.devgenie.model.SonarBaseComponentMetrics;
import com.org.devgenie.model.SonarQubeMetricsResponse;
import com.org.devgenie.model.coverage.CoverageData;
import com.org.devgenie.model.coverage.FileCoverageData;
import com.org.devgenie.mongo.RepositoryAnalysisMongoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CoverageDataService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private JacocoService jacocoService;

    @Value("${coverage.data.use-mongo:true}")
    private boolean useMongoData;

    @Autowired
    private RepositoryAnalysisMongoUtil analysisMongoUtil;

    public SonarQubeMetricsResponse getCurrentCoverage(String repoPath, String branch) {
        log.info("Getting current coverage data for repo: {}", repoPath);

        if (useMongoData) {
            try {
                return getCoverageFromMongo(repoPath,branch);
            } catch (Exception e) {
                log.warn("Failed to get coverage from MongoDB, falling back to Jacoco", e);
                return jacocoService.runAnalysis(repoPath,branch);
            }
        } else {
            return jacocoService.runAnalysis(repoPath,branch);
        }
    }

    public FileCoverageData getFileCoverage(String filePath) {
        Query query = new Query(Criteria.where("filePath").is(filePath));
        FileCoverageData data = mongoTemplate.findOne(query, FileCoverageData.class, "file_coverage");

        if (data == null) {
            log.warn("No coverage data found for file: {}, creating default", filePath);
            return createDefaultFileCoverage(filePath);
        }

        return data;
    }
    public SonarQubeMetricsResponse getCoverageFromMongo(String repoPath, String branch) {
        log.info("Fetching coverage data from MongoDB for repo: {}, branch: {}", repoPath, branch);
        Query query = new Query(Criteria.where("repositoryUrl").is(repoPath).and("branch").is(branch));
        List<CoverageData> coverageDataList = mongoTemplate.find(query, CoverageData.class, "coverage_data");
        log.info("Found {} coverage data entries for repo: {}, branch: {}", coverageDataList.size(), repoPath, branch);

        if (coverageDataList.isEmpty()) {
            log.warn("No coverage data found in MongoDB for repo: {}, branch: {}", repoPath, branch);
            throw new CoverageDataNotFoundException("No coverage data found for repository: " + repoPath + " on branch: " + branch);
        }
        SonarBaseComponentMetrics sonarBaseComponentMetrics = analysisMongoUtil.getSonarBaseComponentMetrics(repoPath, branch);

        if(sonarBaseComponentMetrics == null) {
            log.warn("No SonarQube metrics found for repo: {}, branch: {}", repoPath, branch);
            throw new CoverageDataNotFoundException("No coverage data found for repository: " + repoPath + " on branch: " + branch);
        }

        SonarQubeMetricsResponse sonarQubeMetricsResponse = SonarQubeMetricsResponse.builder()
                .coverageDataList(coverageDataList)
                .sonarBaseComponentMetrics(sonarBaseComponentMetrics)
                .build();

        return sonarQubeMetricsResponse;
    }

    private FileCoverageData createDefaultFileCoverage(String filePath) {
        return FileCoverageData.builder()
                .filePath(filePath)
                .lineCoverage(0.0)
                .branchCoverage(0.0)
                .methodCoverage(0.0)
                .uncoveredLines(new ArrayList<>())
                .uncoveredBranches(new ArrayList<>())
                .totalLines(0)
                .totalBranches(0)
                .totalMethods(0)
                .build();
    }
}
