package com.org.devgenie.input;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.File;

@Component
public class GitInputProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GitInputProcessor.class);

    public void cloneRepository(String url, String branch, File targetDir) throws GitAPIException {
        logger.info("Cloning repository: {} (branch: {}) into: {}", url, branch, targetDir.getAbsolutePath());
        Git.cloneRepository().setURI(url).setBranch(branch).setDirectory(targetDir).call();
    }
}
