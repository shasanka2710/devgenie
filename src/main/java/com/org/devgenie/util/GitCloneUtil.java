package com.org.devgenie.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class GitCloneUtil {

    private static final Logger logger = LoggerFactory.getLogger(GitCloneUtil.class);

    private static final String CLONE_BASE_PATH = "/tmp";  // Path where repo will be cloned in PCF

    /**
     * Clones the GitHub repository to /tmp directory
     * @param repoUrl GitHub repository URL
     * @param branchName Branch to be cloned
     * @return Path to the cloned repo
     * @throws GitAPIException
     * @throws IOException
     */
    public String cloneRepository(String repoUrl, String branchName) throws GitAPIException, IOException {
        // Extract repo name from URL
        String repoName = extractRepoName(repoUrl);
        String targetPath = CLONE_BASE_PATH + "/" + repoName;

        File repoDir = new File(targetPath);

        // Check if repo already exists, delete if present
        if (repoDir.exists()) {
            deleteDirectory(repoDir);
            logger.info("Existing repo deleted: {}", targetPath);
        }

        logger.info("Cloning repo: {} into {}", repoUrl, targetPath);

        // Clone the repo to /tmp/<repoName>
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(repoDir)
                .setBranch(branchName)
                .call()
                .close();

        logger.info("Repository cloned successfully to: {}", targetPath);
        return targetPath;
    }

    /**
     * Extracts the repository name from the GitHub URL
     * @param repoUrl GitHub repository URL
     * @return Name of the repository
     */
    private String extractRepoName(String repoUrl) {
        return repoUrl.substring(repoUrl.lastIndexOf("/") + 1).replace(".git", "");
    }

    /**
     * Recursively deletes directory and its content
     * @param directory Target directory
     */
    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }
}
