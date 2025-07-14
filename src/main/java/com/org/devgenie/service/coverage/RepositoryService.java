package com.org.devgenie.service.coverage;

import com.org.devgenie.exception.coverage.RepositoryException;
import com.org.devgenie.model.coverage.WorkspaceStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class RepositoryService {

    @Value("${coverage.workspace.root-dir:/tmp/coverage-workspaces}")
    private String workspaceRootDir;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GitService gitService;

    /**
     * Clone or update repository in user's workspace
     */
    public String setupRepository(String repositoryUrl, String branch, String workspaceId, String githubToken) {
        String workspaceDir = workspaceRootDir + "/" + workspaceId;
        String repoDir = workspaceDir + "/" + extractRepoName(repositoryUrl);

        try {
            // Create workspace directory
            Files.createDirectories(Paths.get(workspaceDir));

            if (Files.exists(Paths.get(repoDir))) {
                // Update existing repository
                updateRepository(repoDir, branch);
            } else {
                // Clone repository
                cloneRepository(repositoryUrl, repoDir, branch, githubToken);
            }

            return repoDir;

        } catch (Exception e) {
            log.error("Failed to setup repository: {}", repositoryUrl, e);
            throw new RepositoryException("Failed to setup repository: " + e.getMessage(), e);
        }
    }

    /**
     * Read file content from cloned repository
     */
    public String readFileContent(String repoDir, String filePath) {
        try {
            Path fullPath = Paths.get(repoDir, filePath);
            if (!Files.exists(fullPath)) {
                throw new FileNotFoundException("File not found: " + filePath);
            }
            return Files.readString(fullPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to read file: {}", filePath, e);
            throw new RepositoryException("Failed to read file: " + e.getMessage(), e);
        }
    }

    /**
     * Get all Java files in repository (excluding test directories by default)
     */
    public List<String> findJavaFiles(String repoDir, List<String> excludePatterns) {
        try {
            PathMatcher[] excludeMatchers = excludePatterns.stream()
                    .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
                    .toArray(PathMatcher[]::new);

            try (Stream<Path> paths = Files.walk(Paths.get(repoDir))) {
                return paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .filter(path -> !isExcluded(path, excludeMatchers))
                        .map(path -> Paths.get(repoDir).relativize(path).toString())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to find Java files in: {}", repoDir, e);
            throw new RepositoryException("Failed to find Java files: " + e.getMessage(), e);
        }
    }

    /**
     * Get workspace status including repository info and cached data
     */
    public WorkspaceStatusResponse getWorkspaceStatus(String workspaceId) {
        String workspaceDir = workspaceRootDir + "/" + workspaceId;

        WorkspaceStatusResponse.WorkspaceStatusResponseBuilder builder = WorkspaceStatusResponse.builder()
                .workspaceId(workspaceId)
                .workspaceDir(workspaceDir);

        if (Files.exists(Paths.get(workspaceDir))) {
            try {
                // Find repositories in workspace
                List<String> repositories = Files.list(Paths.get(workspaceDir))
                        .filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());

                builder.repositories(repositories)
                        .status("ACTIVE")
                        .lastAccessed(LocalDateTime.now());

            } catch (Exception e) {
                log.warn("Error reading workspace directory", e);
                builder.status("ERROR").error(e.getMessage());
            }
        } else {
            builder.status("NOT_FOUND");
        }

        return builder.build();
    }

    /**
     * Clean up workspace directory
     */
    public void cleanupWorkspace(String workspaceId) {
        String workspaceDir = workspaceRootDir + "/" + workspaceId;

        try {
            if (Files.exists(Paths.get(workspaceDir))) {
                deleteDirectory(Paths.get(workspaceDir));
                log.info("Cleaned up workspace: {}", workspaceId);
            }
        } catch (Exception e) {
            log.error("Failed to cleanup workspace: {}", workspaceId, e);
            throw new RepositoryException("Failed to cleanup workspace: " + e.getMessage(), e);
        }
    }

    private void cloneRepository(String repositoryUrl, String repoDir, String branch, String githubToken)
            throws IOException, InterruptedException {

        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("clone");

        // Add authentication for private repositories
        if (githubToken != null && !githubToken.isEmpty()) {
            String authenticatedUrl = repositoryUrl.replace("https://", "https://" + githubToken + "@");
            command.add(authenticatedUrl);
        } else {
            command.add(repositoryUrl);
        }

        command.add(repoDir);

        if (branch != null && !branch.equals("main") && !branch.equals("master")) {
            command.add("--branch");
            command.add(branch);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RepositoryException("Git clone failed with exit code: " + exitCode);
        }

        log.info("Successfully cloned repository: {} to {}", repositoryUrl, repoDir);
    }

    private void updateRepository(String repoDir, String branch) throws IOException, InterruptedException {
        // Fetch latest changes
        ProcessBuilder pb = new ProcessBuilder("git", "fetch", "origin");
        pb.directory(new File(repoDir));
        Process process = pb.start();
        process.waitFor();

        // Checkout the specified branch
        pb = new ProcessBuilder("git", "checkout", branch != null ? branch : "main");
        pb.directory(new File(repoDir));
        process = pb.start();
        process.waitFor();

        // Pull latest changes
        pb = new ProcessBuilder("git", "pull", "origin", branch != null ? branch : "main");
        pb.directory(new File(repoDir));
        process = pb.start();
        process.waitFor();

        log.info("Successfully updated repository: {}", repoDir);
    }

    private String extractRepoName(String repositoryUrl) {
        // Extract repository name from URL
        String[] parts = repositoryUrl.split("/");
        String repoName = parts[parts.length - 1];
        return repoName.endsWith(".git") ? repoName.substring(0, repoName.length() - 4) : repoName;
    }

    private boolean isExcluded(Path path, PathMatcher[] excludeMatchers) {
        for (PathMatcher matcher : excludeMatchers) {
            if (matcher.matches(path)) {
                return true;
            }
        }
        return false;
    }

    private void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
