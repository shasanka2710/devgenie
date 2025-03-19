package com.org.devgenie.github;

import com.org.devgenie.util.FilePathUtil;
import com.org.devgenie.util.GitCloneUtil;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class GitHubUtility {

    private static final Logger logger = LoggerFactory.getLogger(GitHubUtility.class);

    private final PullRequestHandler pullRequestHandler;
    private final GitCloneUtil gitCloneUtil;
    private final FilePathUtil filePathUtil;

    public GitHubUtility(PullRequestHandler pullRequestHandler, GitCloneUtil gitCloneUtil, FilePathUtil filePathUtil) {
        this.pullRequestHandler = pullRequestHandler;
        this.gitCloneUtil = gitCloneUtil;
        this.filePathUtil = filePathUtil;
    }

    /**
     * Creates a Pull Request after applying fixes
     * @param classNames List of updated class names
     * @param description Pull request description
     * @return URL of the created Pull Request
     * @throws IOException
     */
    public String createPullRequest(List<String> classNames, String description) throws IOException {
        try {
            GHPullRequest pullRequest = pullRequestHandler.createPullRequest(classNames, description);
            if (pullRequest == null) {
                throw new IOException("Error creating pull request");
            }
            logger.info("Pull Request Created:");
            logger.info("Title: {}", pullRequest.getTitle());
            logger.info("Number: {}", pullRequest.getNumber());
            logger.info("URL: {}", pullRequest.getHtmlUrl());
            return pullRequest.getHtmlUrl().toString();
        } catch (IOException e) {
            throw new IOException("Error creating pull request", e);
        }
    }

    /**
     * Clone the GitHub repository to /tmp and return the cloned repo path
     * @param repoUrl GitHub repository URL
     * @param branchName Target branch name
     * @return Path of the cloned repository
     */
    public String cloneRepositoryToTemp(String repoUrl, String branchName) throws GitAPIException, IOException {
        return gitCloneUtil.cloneRepository(repoUrl, branchName);
    }

    /**
     * Get full file path from SonarQube issue and clone repo for file traversal
     * @param repoUrl GitHub repository URL
     * @param branchName Branch name to clone
     * @param sonarFilePath Relative file path reported by SonarQube
     * @return Full path to source file
     */
    public File getFileForSonarIssue(String repoUrl, String branchName, String sonarFilePath) throws GitAPIException, IOException {
        // Step 1: Clone the repository
        String repoPath = cloneRepositoryToTemp(repoUrl, branchName);

        // Step 2: Get full path to the file based on sonar file path
        return filePathUtil.getSourceFile(repoPath, sonarFilePath);
    }

    public static String getRelativePath(String filePath, String clonedPath) {
        // Normalize paths to avoid platform inconsistencies
        Path fullPath = Paths.get(filePath).normalize();
        Path basePath = Paths.get(clonedPath).normalize();

        // Check if the full path starts with the cloned path
        if (fullPath.startsWith(basePath)) {
            // Get the relative path from the base path
            return basePath.relativize(fullPath).toString();
        } else {
            // Return the original path if the base path is not matched
            return filePath;
        }
    }
}