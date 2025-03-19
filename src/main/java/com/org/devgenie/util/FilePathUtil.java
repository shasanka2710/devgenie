package com.org.devgenie.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FilePathUtil {

    private static final Logger logger = LoggerFactory.getLogger(FilePathUtil.class);

    /**
     * Get the full path of the file in the cloned repo based on the SonarQube issue file path
     * @param repoPath Path to the cloned repo
     * @param sonarFilePath Relative file path reported by SonarQube (e.g., src/com/org/devgenie/Main.java)
     * @return Full path to the source file
     */
    public File getSourceFile(String repoPath, String sonarFilePath) {
        Path fullPath = Paths.get(repoPath, sonarFilePath);
        File targetFile = fullPath.toFile();

        if (targetFile.exists()) {
            logger.info("File found: {}", targetFile.getAbsolutePath());
            return targetFile;
        } else {
            logger.error("File not found: {}", targetFile.getAbsolutePath());
            return null;
        }
    }
}
