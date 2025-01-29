package com.org.javadoc.ai.generator.github;

import com.org.javadoc.ai.generator.config.GitHubConfig;
import com.org.javadoc.ai.generator.util.PathConverter;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class PullRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(PullRequestHandler.class);

    @Autowired
    private GitHubConfig config;

    @Autowired
    private GitHub github;

    // Maximum retry attempts
    private static final int MAX_RETRIES = 5;

    // Base delay in milliseconds
    private static final long BASE_DELAY_MS = 1000;

    /**
     * Creates a pull request for the given class names and description with retry logic.
     *
     * @param classNames  List of fully qualified class names.
     * @param description Description of the fix.
     * @throws IOException If an error occurs during GitHub operations after retries.
     */
    public GHPullRequest createPullRequest(List<String> classNames, String description) throws IOException {
        GHRepository repo = github.getRepository(config.getRepository());
        String defaultBranch = config.getDefaultBranch();
        // Generate dynamic branch name based on the current timestamp
        String branchName = generateDynamicBranchName();
        // Create branch from the default branch with retry
        retryOperation(() -> {
            createBranchFromDefaultBranch(repo, branchName, defaultBranch);
            // Return null as it's a void operation
            return null;
        }, "Creating branch", MAX_RETRIES);
        // Get the list of files to be updated
        List<String> filePaths = classNames.stream().map(PathConverter::toSlashedPath).toList();
        for (String filePath : filePaths) {
            // Update file with retry
            retryOperation(() -> {
                updateFileInBranch(repo, branchName, filePath, description);
                // Return null as it's a void operation
                return null;
            }, "Updating file: " + filePath, MAX_RETRIES);
        }
        // Create a pull request with retry
        return retryOperation(() -> {
            // Use String.format for better readability
            return repo.createPullRequest(String.format("Apply fix: %s", description), branchName, defaultBranch, String.format("This PR applies the following fix:\n\n%s", description));
        }, "Creating pull request", MAX_RETRIES);
    }

    /**
     * Generates a dynamic branch name based on a timestamp.
     *
     * @return A unique branch name.
     */
    private String generateDynamicBranchName() {
        // Short UUID
        return "fix-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Creates a branch from the default branch.
     *
     * @param repo        The GitHub repository.
     * @param branchName  The branch name to create.
     * @param defaultBranch The default branch name.
     * @throws IOException If an error occurs during branch creation.
     */
    private void createBranchFromDefaultBranch(GHRepository repo, String branchName, String defaultBranch) throws IOException {
        try {
            // Check if the branch already exists, if it does, throw an exception to avoid duplication
            GHRef ref = repo.getRef("heads/" + branchName);
            if (ref != null) {
                throw new IOException("Branch already exists: " + branchName);
            }
        } catch (GHFileNotFoundException e) {
            // Branch doesn't exist, proceed with creation
            GHRef defaultRef = repo.getRef("heads/" + defaultBranch);
            repo.createRef("refs/heads/" + branchName, defaultRef.getObject().getSha());
        }
    }

    /**
     * Retry logic to handle transient failures.
     *
     * @param operation The operation to execute.
     * @param operationName The name of the operation for logging.
     * @param maxRetries The maximum number of retry attempts.
     * @param <T> The type of the return value.
     * @return The result of the operation.
     * @throws IOException If the operation fails after all retries.
     */
    private <T> T retryOperation(RetryableOperation<T> operation, String operationName, int maxRetries) throws IOException {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                // Call the lambda function here
                return operation.execute();
            } catch (IOException e) {
                attempt++;
                logger.error("{} failed on attempt {}: {}", operationName, attempt, e.getMessage());
                if (attempt >= maxRetries) {
                    throw new IOException(operationName + " failed after " + attempt + " attempts.", e);
                }
                // Exponential backoff
                // Increase delay with each attempt
                long delay = BASE_DELAY_MS * (long) Math.pow(2, attempt);
                System.out.println(operationName + " failed. Retrying in " + delay + " ms...");
                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", ie);
                }
            }
        }
        throw new IOException(operationName + " failed after " + maxRetries + " retries.");
    }

    /**
     * Updates the content of a file in the given branch.
     *
     * @param repo        The GitHub repository.
     * @param branchName  The branch to update the file in.
     * @param filePath    The path to the file to update.
     * @param description The description for the commit message.
     * @throws IOException If an error occurs during file update.
     */
    private void updateFileInBranch(GHRepository repo, String branchName, String filePath, String description) throws IOException {
        // Get the content of the file
        String fileContent = Files.readString(Paths.get(filePath));
        // Retrieve the current sha of the file (if it exists)
        GHContent existingFile = getFileFromRepo(repo, filePath);
        String sha = (existingFile != null) ? existingFile.getSha() : null;
        // Add or update the file in the branch
        if (sha != null) {
            // If the file exists, update it
            logger.info("File exists: " + filePath);
            repo.createContent().path(filePath).content(fileContent).message("Apply fix: " + description).sha(// Provide sha for existing file
            sha).branch(branchName).commit();
        } else {
            // If the file doesn't exist, create it
            logger.info("File doesn't exist: " + filePath);
            repo.createContent().path(filePath).content(fileContent).message("Apply fix: " + description).branch(branchName).commit();
        }
    }

    /**
     * Retrieves the content of a file from the repository.
     *
     * @param repo     The GitHub repository.
     * @param filePath The file path.
     * @return The GitHub content object for the file.
     * @throws IOException If an error occurs during retrieval.
     */
    private GHContent getFileFromRepo(GHRepository repo, String filePath) throws IOException {
        try {
            return repo.getFileContent(filePath);
        } catch (FileNotFoundException e) {
            // Return null if the file does not exist
            return null;
        }
    }

    /**
     * Functional interface for retryable operations.
     *
     * @param <T> The return type of the operation.
     */
    @FunctionalInterface
    private interface RetryableOperation<T> {

        T execute() throws IOException;
    }
}
