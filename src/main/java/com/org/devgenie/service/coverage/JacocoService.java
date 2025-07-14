package com.org.devgenie.service.coverage;

import com.org.devgenie.exception.coverage.JacocoException;
import com.org.devgenie.model.coverage.CoverageData;
import com.org.devgenie.model.coverage.FileCoverageData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class JacocoService {

    @Value("${jacoco.exec.path:target/jacoco.exec}")
    private String jacocoExecPath;

    @Value("${maven.command:mvn}")
    private String mavenCommand;

    public CoverageData runAnalysis(String repoPath) {
        log.info("Running Jacoco analysis for repo: {}", repoPath);

        try {
            // Run Maven test with Jacoco
            runMavenJacocoTest(repoPath);

            // Parse Jacoco XML report
            return parseJacocoReport(repoPath);

        } catch (Exception e) {
            log.error("Failed to run Jacoco analysis", e);
            throw new JacocoException("Failed to run Jacoco analysis: " + e.getMessage(), e);
        }
    }

    private void runMavenJacocoTest(String repoPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                mavenCommand, "clean", "test", "jacoco:report"
        );
        pb.directory(new File(repoPath));
        pb.inheritIO();

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new JacocoException("Maven Jacoco test failed with exit code: " + exitCode);
        }

        log.info("Jacoco analysis completed successfully");
    }

    private CoverageData parseJacocoReport(String repoPath) throws IOException {
        String reportPath = repoPath + "/target/site/jacoco/jacoco.xml";
        File reportFile = new File(reportPath);

        if (!reportFile.exists()) {
            throw new JacocoException("Jacoco report not found at: " + reportPath);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(reportFile);

            return parseJacocoXml(document, repoPath);

        } catch (Exception e) {
            throw new JacocoException("Failed to parse Jacoco XML report", e);
        }
    }

    private CoverageData parseJacocoXml(Document document, String repoPath) {
        CoverageData.CoverageDataBuilder builder = CoverageData.builder();
        builder.repoPath(repoPath);
        builder.timestamp(LocalDateTime.now());

        List<FileCoverageData> files = new ArrayList<>();
        NodeList packageNodes = document.getElementsByTagName("package");

        int totalLines = 0, coveredLines = 0;
        int totalBranches = 0, coveredBranches = 0;
        int totalMethods = 0, coveredMethods = 0;

        for (int i = 0; i < packageNodes.getLength(); i++) {
            Element packageElement = (Element) packageNodes.item(i);
            NodeList classNodes = packageElement.getElementsByTagName("class");

            for (int j = 0; j < classNodes.getLength(); j++) {
                Element classElement = (Element) classNodes.item(j);
                String sourceFileName = classElement.getAttribute("sourcefilename");

                if (sourceFileName.endsWith(".java")) {
                    FileCoverageData fileData = parseClassCoverage(classElement, packageElement.getAttribute("name"));
                    files.add(fileData);

                    totalLines += fileData.getTotalLines();
                    coveredLines += fileData.getCoveredLines();
                    totalBranches += fileData.getTotalBranches();
                    coveredBranches += fileData.getCoveredBranches();
                    totalMethods += fileData.getTotalMethods();
                    coveredMethods += fileData.getCoveredMethods();
                }
            }
        }

        double overallLineCoverage = totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0;
        double overallBranchCoverage = totalBranches > 0 ? (double) coveredBranches / totalBranches * 100 : 0;
        double overallMethodCoverage = totalMethods > 0 ? (double) coveredMethods / totalMethods * 100 : 0;

        return builder
                .files(files)
                .overallCoverage(overallLineCoverage)
                .lineCoverage(overallLineCoverage)
                .branchCoverage(overallBranchCoverage)
                .methodCoverage(overallMethodCoverage)
                .totalLines(totalLines)
                .coveredLines(coveredLines)
                .totalBranches(totalBranches)
                .coveredBranches(coveredBranches)
                .totalMethods(totalMethods)
                .coveredMethods(coveredMethods)
                .build();
    }

    private FileCoverageData parseClassCoverage(Element classElement, String packageName) {
        String className = classElement.getAttribute("name");
        String filePath = packageName.replace('.', '/') + "/" + className + ".java";

        NodeList counterNodes = classElement.getElementsByTagName("counter");

        int totalLines = 0, coveredLines = 0;
        int totalBranches = 0, coveredBranches = 0;
        int totalMethods = 0, coveredMethods = 0;

        for (int i = 0; i < counterNodes.getLength(); i++) {
            Element counter = (Element) counterNodes.item(i);
            String type = counter.getAttribute("type");
            int missed = Integer.parseInt(counter.getAttribute("missed"));
            int covered = Integer.parseInt(counter.getAttribute("covered"));

            switch (type) {
                case "LINE":
                    totalLines = missed + covered;
                    coveredLines = covered;
                    break;
                case "BRANCH":
                    totalBranches = missed + covered;
                    coveredBranches = covered;
                    break;
                case "METHOD":
                    totalMethods = missed + covered;
                    coveredMethods = covered;
                    break;
            }
        }

        double lineCoverage = totalLines > 0 ? (double) coveredLines / totalLines * 100 : 0;
        double branchCoverage = totalBranches > 0 ? (double) coveredBranches / totalBranches * 100 : 0;
        double methodCoverage = totalMethods > 0 ? (double) coveredMethods / totalMethods * 100 : 0;

        return FileCoverageData.builder()
                .filePath(filePath)
                .lineCoverage(lineCoverage)
                .branchCoverage(branchCoverage)
                .methodCoverage(methodCoverage)
                .totalLines(totalLines)
                .coveredLines(coveredLines)
                .totalBranches(totalBranches)
                .coveredBranches(coveredBranches)
                .totalMethods(totalMethods)
                .coveredMethods(coveredMethods)
                .uncoveredLines(extractUncoveredLines(classElement))
                .uncoveredBranches(extractUncoveredBranches(classElement))
                .build();
    }

    private List<String> extractUncoveredLines(Element classElement) {
        List<String> uncoveredLines = new ArrayList<>();
        NodeList lineNodes = classElement.getElementsByTagName("line");

        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element line = (Element) lineNodes.item(i);
            if ("0".equals(line.getAttribute("ci"))) { // ci = covered instructions
                uncoveredLines.add(line.getAttribute("nr"));
            }
        }

        return uncoveredLines;
    }

    private List<String> extractUncoveredBranches(Element classElement) {
        List<String> uncoveredBranches = new ArrayList<>();
        NodeList lineNodes = classElement.getElementsByTagName("line");

        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element line = (Element) lineNodes.item(i);
            String mb = line.getAttribute("mb"); // missed branches
            if (!mb.isEmpty() && Integer.parseInt(mb) > 0) {
                uncoveredBranches.add(line.getAttribute("nr"));
            }
        }

        return uncoveredBranches;
    }
}
