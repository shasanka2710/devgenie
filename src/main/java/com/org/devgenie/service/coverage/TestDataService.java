package com.org.devgenie.service.coverage;

import com.org.devgenie.model.coverage.CoverageData;
import com.org.devgenie.model.coverage.ProjectConfiguration;
import com.org.devgenie.mongo.CoverageDataFlatMongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class TestDataService {

    @Autowired
    private CoverageDataFlatMongoRepository coverageRepository;

    public void createTestCoverageData() {
        String repoPath = "https://github.com/shasanka2710/devgenie";
        String branch = "main";
        LocalDateTime now = LocalDateTime.now();

        ProjectConfiguration projectConfig = ProjectConfiguration.builder()
                .buildTool("gradle")
                .testFramework("junit5")
                .javaVersion("21")
                .isSpringBoot(true)
                .detectedAt(now)
                .build();

        List<CoverageData> testData = Arrays.asList(
                // Root directory
                CoverageData.builder()
                        .branch(branch)
                        .type("DIRECTORY")
                        .repoPath(repoPath)
                        .path("src")
                        .parentPath("")
                        .name("src")
                        .timestamp(now)
                        .projectConfiguration(projectConfig)
                        .coverageSource(CoverageData.CoverageSource.SONARQUBE)
                        .overallCoverage(73.5)
                        .lineCoverage(76.2)
                        .branchCoverage(68.1)
                        .methodCoverage(82.4)
                        .totalLines(500)
                        .coveredLines(381)
                        .totalBranches(120)
                        .coveredBranches(82)
                        .totalMethods(45)
                        .coveredMethods(37)
                        .directoryName("src")
                        .children(Arrays.asList("src/main", "src/test"))
                        .build(),

                // Main directory
                CoverageData.builder()
                        .branch(branch)
                        .type("DIRECTORY")
                        .repoPath(repoPath)
                        .path("src/main")
                        .parentPath("src")
                        .name("main")
                        .timestamp(now)
                        .projectConfiguration(projectConfig)
                        .coverageSource(CoverageData.CoverageSource.SONARQUBE)
                        .overallCoverage(70.0)
                        .lineCoverage(72.0)
                        .branchCoverage(65.0)
                        .methodCoverage(75.0)
                        .totalLines(400)
                        .coveredLines(288)
                        .totalBranches(100)
                        .coveredBranches(65)
                        .totalMethods(35)
                        .coveredMethods(26)
                        .directoryName("main")
                        .children(Arrays.asList("src/main/java"))
                        .build(),

                // Java directory
                CoverageData.builder()
                        .branch(branch)
                        .type("DIRECTORY")
                        .repoPath(repoPath)
                        .path("src/main/java")
                        .parentPath("src/main")
                        .name("java")
                        .timestamp(now)
                        .projectConfiguration(projectConfig)
                        .coverageSource(CoverageData.CoverageSource.SONARQUBE)
                        .overallCoverage(70.0)
                        .lineCoverage(72.0)
                        .branchCoverage(65.0)
                        .methodCoverage(75.0)
                        .totalLines(400)
                        .coveredLines(288)
                        .totalBranches(100)
                        .coveredBranches(65)
                        .totalMethods(35)
                        .coveredMethods(26)
                        .directoryName("java")
                        .children(Arrays.asList("src/main/java/com"))
                        .build(),

                // Package directory
                CoverageData.builder()
                        .branch(branch)
                        .type("DIRECTORY")
                        .repoPath(repoPath)
                        .path("src/main/java/com")
                        .parentPath("src/main/java")
                        .name("com")
                        .timestamp(now)
                        .projectConfiguration(projectConfig)
                        .coverageSource(CoverageData.CoverageSource.SONARQUBE)
                        .overallCoverage(70.0)
                        .lineCoverage(72.0)
                        .branchCoverage(65.0)
                        .methodCoverage(75.0)
                        .totalLines(400)
                        .coveredLines(288)
                        .totalBranches(100)
                        .coveredBranches(65)
                        .totalMethods(35)
                        .coveredMethods(26)
                        .directoryName("com")
                        .children(Arrays.asList("src/main/java/com/org"))
                        .build(),

                // Service package directory
                CoverageData.builder()
                        .branch(branch)
                        .type("DIRECTORY")
                        .repoPath(repoPath)
                        .path("src/main/java/com/org/devgenie/service")
                        .parentPath("src/main/java/com/org/devgenie")
                        .name("service")
                        .timestamp(now)
                        .projectConfiguration(projectConfig)
                        .coverageSource(CoverageData.CoverageSource.SONARQUBE)
                        .overallCoverage(85.0)
                        .lineCoverage(87.0)
                        .branchCoverage(80.0)
                        .methodCoverage(90.0)
                        .totalLines(200)
                        .coveredLines(174)
                        .totalBranches(50)
                        .coveredBranches(40)
                        .totalMethods(20)
                        .coveredMethods(18)
                        .directoryName("service")
                        .children(Arrays.asList(
                                "src/main/java/com/org/devgenie/service/UserService.java",
                                "src/main/java/com/org/devgenie/service/CoverageService.java",
                                "src/main/java/com/org/devgenie/service/GitService.java"
                        ))
                        .build(),

                // Sample files
                CoverageData.builder()
                        .branch(branch)
                        .type("FILE")
                        .repoPath(repoPath)
                        .path("src/main/java/com/org/devgenie/service/UserService.java")
                        .parentPath("src/main/java/com/org/devgenie/service")
                        .name("UserService.java")
                        .timestamp(now)
                        .projectConfiguration(projectConfig)
                        .coverageSource(CoverageData.CoverageSource.SONARQUBE)
                        .overallCoverage(65.2)
                        .lineCoverage(65.2)
                        .branchCoverage(58.7)
                        .methodCoverage(72.1)
                        .totalLines(80)
                        .coveredLines(52)
                        .totalBranches(15)
                        .coveredBranches(9)
                        .totalMethods(8)
                        .coveredMethods(6)
                        .fileName("UserService.java")
                        .className("UserService")
                        .packageName("com.org.devgenie.service")
                        .build(),

                CoverageData.builder()
                        .branch(branch)
                        .type("FILE")
                        .repoPath(repoPath)
                        .path("src/main/java/com/org/devgenie/service/CoverageService.java")
                        .parentPath("src/main/java/com/org/devgenie/service")
                        .name("CoverageService.java")
                        .timestamp(now)
                        .projectConfiguration(projectConfig)
                        .coverageSource(CoverageData.CoverageSource.SONARQUBE)
                        .overallCoverage(92.3)
                        .lineCoverage(94.1)
                        .branchCoverage(88.9)
                        .methodCoverage(95.0)
                        .totalLines(85)
                        .coveredLines(80)
                        .totalBranches(18)
                        .coveredBranches(16)
                        .totalMethods(10)
                        .coveredMethods(10)
                        .fileName("CoverageService.java")
                        .className("CoverageService")
                        .packageName("com.org.devgenie.service")
                        .build(),

                CoverageData.builder()
                        .branch(branch)
                        .type("FILE")
                        .repoPath(repoPath)
                        .path("src/main/java/com/org/devgenie/service/GitService.java")
                        .parentPath("src/main/java/com/org/devgenie/service")
                        .name("GitService.java")
                        .timestamp(now)
                        .projectConfiguration(projectConfig)
                        .coverageSource(CoverageData.CoverageSource.SONARQUBE)
                        .overallCoverage(45.8)
                        .lineCoverage(48.2)
                        .branchCoverage(41.7)
                        .methodCoverage(47.5)
                        .totalLines(120)
                        .coveredLines(58)
                        .totalBranches(24)
                        .coveredBranches(10)
                        .totalMethods(12)
                        .coveredMethods(6)
                        .fileName("GitService.java")
                        .className("GitService")
                        .packageName("com.org.devgenie.service")
                        .build()
        );

        // Clear existing data and insert test data
        coverageRepository.deleteAll();
        coverageRepository.saveAll(testData);
        
        log.info("Created {} test coverage data records", testData.size());
    }
}
