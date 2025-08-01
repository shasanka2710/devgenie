package com.org.devgenie.service.coverage;

import com.org.devgenie.exception.coverage.GitException;
import com.org.devgenie.model.coverage.CoverageData;
import com.org.devgenie.model.coverage.FileChange;
import com.org.devgenie.model.coverage.PullRequestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GitService {

    @Value("${git.command:git}")
    private String gitCommand;

    @Value("${github.token:}")
    private String githubToken;

    @Value("${github.api.url:https://api.github.com}")
    private String githubApiUrl;

    @Autowired
    private RestTemplate restTemplate;

    public void applyChanges(List<FileChange> changes,String workspaceDir) {
        log.info("Applying {} file changes", changes.size());

        for (FileChange change : changes) {
            try {
                applyFileChange(change);
            } catch (Exception e) {
                log.error("Failed to apply change for file: {}", change.getFilePath(), e);
                throw new GitException("Failed to apply changes", e);
            }
        }
    }

    public PullRequestResult createPullRequest(String sessionId, CoverageData finalCoverage, String repoDir) {
        log.info("Creating pull request for session: {} in directory: {}", sessionId, repoDir);

        try {
            // Create branch in the workspace directory
            String branchName = "coverage-improvement-" + sessionId;
            createBranch(branchName, repoDir);

            // Commit changes in the workspace directory
            commitChanges(sessionId, finalCoverage, repoDir);

            // Push branch from the workspace directory
            pushBranch(branchName, repoDir);

            // Create PR via GitHub API
            return createGitHubPullRequest(branchName, sessionId, finalCoverage, repoDir);

        } catch (Exception e) {
            log.error("Failed to create pull request", e);
            throw new GitException("Failed to create pull request: " + e.getMessage(), e);
        }
    }

    public void rollbackChanges(String sessionId) {
        log.info("Rolling back changes for session: {}", sessionId);

        try {
            executeGitCommandInCurrentDir("reset", "--hard", "HEAD");
            executeGitCommandInCurrentDir("clean", "-fd");
            log.info("Changes rolled back successfully");
        } catch (Exception e) {
            log.error("Failed to rollback changes", e);
            throw new GitException("Failed to rollback changes", e);
        }
    }

    /**
     * Pull latest changes from remote repository
     * @param repoDir the repository directory
     * @param branch the branch to pull from
     */
    public void pullLatestChanges(String repoDir, String branch) {
        log.info("Pulling latest changes for branch: {} in directory: {}", branch, repoDir);

        try {
            // Change to the repository directory and pull latest changes
            ProcessBuilder pb = new ProcessBuilder(gitCommand, "pull", "origin", branch);
            pb.directory(new java.io.File(repoDir));
            Process process = pb.start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("Git pull failed with exit code: {}, continuing with existing repository", exitCode);
            } else {
                log.info("Successfully pulled latest changes for branch: {}", branch);
            }
        } catch (Exception e) {
            log.warn("Failed to pull latest changes, using existing repository: {}", e.getMessage());
            // Don't throw exception - this is a best-effort operation
        }
    }

    private void applyFileChange(FileChange change) throws IOException {
        switch (change.getChangeType()) {
            case TEST_ADDED:
                writeTestFile(change);
                break;
            case TEST_MODIFIED:
                modifyTestFile(change);
                break;
            case SOURCE_MODIFIED:
                modifySourceFile(change);
                break;
            default:
                log.warn("Unknown change type: {}", change.getChangeType());
        }
    }

    private void writeTestFile(FileChange change) throws IOException {
        Path testFilePath = Paths.get(change.getTestFilePath());

        // Create directories if they don't exist
        Files.createDirectories(testFilePath.getParent());

        // Write test content
        Files.writeString(testFilePath, change.getContent(), StandardCharsets.UTF_8);

        log.info("Created test file: {}", change.getTestFilePath());
    }

    private void modifyTestFile(FileChange change) throws IOException {
        Path testFilePath = Paths.get(change.getTestFilePath());

        if (Files.exists(testFilePath)) {
            // Append to existing test file
            String existingContent = Files.readString(testFilePath);
            String updatedContent = mergeTestContent(existingContent, change.getContent());
            Files.writeString(testFilePath, updatedContent, StandardCharsets.UTF_8);
        } else {
            // Create new file
            writeTestFile(change);
        }

        log.info("Modified test file: {}", change.getTestFilePath());
    }

    private void modifySourceFile(FileChange change) throws IOException {
        Path sourceFilePath = Paths.get(change.getFilePath());
        Files.writeString(sourceFilePath, change.getContent(), StandardCharsets.UTF_8);
        log.info("Modified source file: {}", change.getFilePath());
    }

    private String mergeTestContent(String existingContent, String newContent) {
        // Simple merge strategy - append new test methods before the closing brace
        int lastBraceIndex = existingContent.lastIndexOf('}');
        if (lastBraceIndex > 0) {
            // Extract new test methods from newContent
            String newMethods = extractTestMethods(newContent);
            return existingContent.substring(0, lastBraceIndex) + "\n" + newMethods + "\n}";
        }

        return newContent; // Fallback to new content
    }

    private String extractTestMethods(String testContent) {
        // Extract test methods (simplified - you might want to use a proper Java parser)
        Pattern testMethodPattern = Pattern.compile("@Test[\\s\\S]*?\\n\\s*}", Pattern.MULTILINE);
        Matcher matcher = testMethodPattern.matcher(testContent);

        StringBuilder methods = new StringBuilder();
        while (matcher.find()) {
            methods.append(matcher.group()).append("\n\n");
        }

        return methods.toString();
    }

    private void createBranch(String branchName, String repoDir) throws IOException, InterruptedException {
        executeGitCommand(repoDir, "checkout", "-b", branchName);
    }

    private void commitChanges(String sessionId, CoverageData finalCoverage, String repoDir) throws IOException, InterruptedException {
        executeGitCommand(repoDir, "add", ".");

        String commitMessage = String.format(
                "feat: Improve code coverage by %.2f%%\n\nSession: %s\nOverall coverage: %.2f%% -> %.2f%%",
                finalCoverage.getOverallCoverage() - getPreviousCoverage(),
                sessionId,
                getPreviousCoverage(),
                finalCoverage.getOverallCoverage()
        );

        executeGitCommand(repoDir, "commit", "-m", commitMessage);
    }

    private void pushBranch(String branchName, String repoDir) throws IOException, InterruptedException {
        executeGitCommand(repoDir, "push", "origin", branchName);
    }

    private PullRequestResult createGitHubPullRequest(String branchName, String sessionId, CoverageData finalCoverage, String repoDir) {
        try {
            String repoInfo = getRepositoryInfo(repoDir);
            String[] repoParts = repoInfo.split("/");
            String owner = repoParts[0];
            String repo = repoParts[1];

            Map<String, Object> prRequest = createPRRequestBody(branchName, sessionId, finalCoverage);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + githubToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(prRequest, headers);

            String url = githubApiUrl + "/repos/" + owner + "/" + repo + "/pulls";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                org.springframework.http.HttpMethod.POST, 
                entity, 
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> prData = response.getBody();

            return PullRequestResult.builder()
                    .prNumber(((Number) prData.get("number")).intValue())
                    .prUrl((String) prData.get("html_url"))
                    .branchName(branchName)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Failed to create GitHub PR", e);
            return PullRequestResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .branchName(branchName)
                    .build();
        }
    }

    private Map<String, Object> createPRRequestBody(String branchName, String sessionId, CoverageData finalCoverage) {
        Map<String, Object> prRequest = new HashMap<>();
        prRequest.put("title", "Automated Code Coverage Improvement - " + sessionId);
        prRequest.put("head", branchName);
        prRequest.put("base", "main");

        String description = String.format("""
            ## 🚀 Automated Code Coverage Enhancement
            
            **Session ID:** %s
            **Coverage Improvement:** %.2f%% → %.2f%% (+%.2f%%)
            
            ### 📊 Coverage Metrics
            - **Line Coverage:** %.2f%%
            - **Branch Coverage:** %.2f%%
            - **Method Coverage:** %.2f%%
          
            ### 🤖 Generated by Coverage Agent
            This PR was automatically generated to improve code coverage using AI-powered test generation.
            
            **Review Notes:**
            - All tests are automatically generated
            - Please review test quality and assertions
            - Verify test coverage actually improved as expected
            """,
                sessionId,
                getPreviousCoverage(),
                finalCoverage.getOverallCoverage(),
                finalCoverage.getOverallCoverage() - getPreviousCoverage(),
                finalCoverage.getLineCoverage(),
                finalCoverage.getBranchCoverage(),
                finalCoverage.getMethodCoverage()
        );

        prRequest.put("body", description);

        return prRequest;
    }

    private String getRepositoryInfo(String repoDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(gitCommand, "remote", "get-url", "origin");
        pb.directory(new java.io.File(repoDir));
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String remoteUrl = reader.readLine();

            // Extract owner/repo from URL (e.g., git@github.com:owner/repo.git)
            if (remoteUrl.contains("github.com")) {
                String[] parts = remoteUrl.split("[:/]");
                String repo = parts[parts.length - 1].replace(".git", "");
                String owner = parts[parts.length - 2];
                return owner + "/" + repo;
            }
        }

        throw new GitException("Could not extract repository information from remote URL");
    }

    private double getPreviousCoverage() {
        // This should be stored/retrieved from the session context
        return 65.0; // Placeholder
    }

    private void executeGitCommand(String repoDir, String... commands) throws IOException, InterruptedException {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(gitCommand);
        fullCommand.addAll(Arrays.asList(commands));

        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.directory(new java.io.File(repoDir));
        Process process = pb.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new GitException("Git command failed with exit code: " + exitCode);
        }
    }

    // Keep the old method for backwards compatibility (current directory)
    private void executeGitCommandInCurrentDir(String... commands) throws IOException, InterruptedException {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(gitCommand);
        fullCommand.addAll(Arrays.asList(commands));

        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        Process process = pb.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new GitException("Git command failed with exit code: " + exitCode);
        }
    }

    /**
     * Test method to verify PR creation workflow
     * @param sessionId test session ID
     * @param repoDir repository directory
     * @return test result
     */
    public PullRequestResult testPullRequestCreation(String sessionId, String repoDir) {
        log.info("Testing PR creation for session: {} in directory: {}", sessionId, repoDir);
        
        try {
            // First validate GitHub token
            if (!validateGitHubToken()) {
                throw new GitException("GitHub token validation failed during test");
            }
            
            // Test repository info extraction
            String repoInfo = getRepositoryInfo(repoDir);
            log.info("Repository info extracted: {}", repoInfo);
            
            // Create dummy coverage data for testing
            CoverageData testCoverage = CoverageData.builder()
                    .overallCoverage(85.0)
                    .lineCoverage(85.0)
                    .branchCoverage(80.0)
                    .methodCoverage(90.0)
                    .build();
            
            // Test the PR creation process (without actually creating git branch/commits)
            String branchName = "coverage-improvement-test-" + sessionId;
            Map<String, Object> prRequest = createPRRequestBody(branchName, sessionId, testCoverage);
            
            log.info("Test PR request created successfully: {}", prRequest.get("title"));
            
            return PullRequestResult.builder()
                    .success(true)
                    .branchName(branchName)
                    .prNumber(0) // Test mode
                    .prUrl("https://github.com/test/pr/0")
                    .build();
                    
        } catch (Exception e) {
            log.error("PR creation test failed", e);
            return PullRequestResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .branchName("test-branch-" + sessionId)
                    .build();
        }
    }

    /**
     * Validates the GitHub token by making a test API call
     * @return true if token is valid, false otherwise
     */
    public boolean validateGitHubToken() {
        if (githubToken == null || githubToken.trim().isEmpty()) {
            log.error("GitHub token is not configured");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + githubToken);
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("X-GitHub-Api-Version", "2022-11-28");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Test with user endpoint
            String url = githubApiUrl + "/user";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> userData = response.getBody();
                log.info("GitHub token validated successfully for user: {}", userData.get("login"));
                return true;
            } else {
                log.error("GitHub token validation failed with status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to validate GitHub token", e);
            return false;
        }
    }

    private boolean validateGitHubToken_Simple() {
        // Simple validation - just check if token is not empty
        return githubToken != null && !githubToken.trim().isEmpty();
    }
}
