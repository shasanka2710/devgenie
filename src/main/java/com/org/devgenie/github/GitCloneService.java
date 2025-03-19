package com.org.devgenie.github;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
public class GitCloneService {

    @Value("${github.repo.url}")
    private String repoUrl;

    @Value("${github.cloned.repo.path}")
    private String clonedRepoPath;

    @Value("${github.repo.branch:main}")
    private String repoBranch;

    @Value("${github.username}")
    private String githubUsername;

    @Value("${github.token}")
    private String githubToken;

    @PostConstruct
    public void cloneRepositoryAtStartup() {
        try {
            File cloneDir = new File(clonedRepoPath);

            // Check if directory already exists
            if (cloneDir.exists() && cloneDir.list().length!=0 && Files.isDirectory(Paths.get(clonedRepoPath))) {
                log.info("✅ Repository already cloned at: {}", clonedRepoPath);
                // Optionally, pull the latest changes from remote to sync
                pullLatestChanges(cloneDir);
            } else {
                log.info("⚡ Cloning repository from {} to {}", repoUrl, clonedRepoPath);
                cloneRepository(repoUrl, clonedRepoPath, repoBranch);
                log.info("✅ Cloning completed!");
            }
        } catch (Exception e) {
            log.error("❌ Error while cloning repository: {}", e.getMessage());
        }
    }

    private void cloneRepository(String repoUrl, String cloneDir, String branch) throws GitAPIException {
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(cloneDir))
                .setBranch(branch)
                .setDepth(1).setCloneAllBranches(false);

        // Add authentication if username and token are available
        if (githubUsername != null && githubToken != null) {
            log.info("🔐 Using authenticated clone for repo...");
            cloneCommand.setCredentialsProvider(
                    new UsernamePasswordCredentialsProvider(githubUsername, githubToken));
        }

        try (Git git = cloneCommand.call()) {
            log.info("✅ Cloned branch '{}' to path '{}'", branch, cloneDir);
        }
    }

    private void pullLatestChanges(File repoDir) {
        try (Git git = Git.open(repoDir)) {
            log.info("🔄 Pulling latest changes from remote...");
            git.pull().call();
            log.info("✅ Repository is up to date.");
        } catch (IOException | GitAPIException e) {
            log.error("❌ Failed to pull latest changes: {}", e.getMessage());
        }
    }
}
