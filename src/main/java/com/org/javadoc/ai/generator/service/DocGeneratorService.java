package com.org.javadoc.ai.generator.service;


import com.org.javadoc.ai.generator.config.AppConfig;
import com.org.javadoc.ai.generator.input.GitInputProcessor;
import com.org.javadoc.ai.generator.input.LocalFileInputProcessor;
import com.org.javadoc.ai.generator.parser.JavaCodeParser;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class DocGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(DocGeneratorService.class);

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private JavaCodeParser javaCodeParser;

    @Autowired
    private LocalFileInputProcessor localFileInputProcessor;

    @Autowired
    private GitInputProcessor gitInputProcessor;

    public void generateDocs(String inputType,
                             String source,
                             String configFilePath,
                             String gitBranch) throws IOException, GitAPIException {
        // not using configfilepath as of now, we can use to save specific path in config file
        List<File> javaFiles;

        switch (inputType) {
            case "local":
                javaFiles = localFileInputProcessor.processInput(source);
                break;
            case "git":
                String tempDirPath = appConfig.getTempDir() + File.separator + "repo";
                File tempDir = new File(tempDirPath);
                FileSystemUtils.deleteRecursively(tempDir); // Clean up from previous runs
                tempDir.mkdirs();

               gitInputProcessor.cloneRepository(source, gitBranch, tempDir);
                javaFiles = localFileInputProcessor.processInput(tempDirPath); // Reuse local file processor
                break;
            default:
                throw new IllegalArgumentException("Invalid input type: " + inputType);
        }
        for (File javaFile : javaFiles) {
            try {
                if (shouldSkipFile(javaFile, appConfig)) {
                    logger.info("Skipping file: " + javaFile.getAbsolutePath());
                    continue;
                }
                javaCodeParser.parseAndGenerateDocs(javaFile);
            } catch (Exception e) {
                logger.error("Error processing file: " + javaFile.getAbsolutePath(), e);
            }
        }
    }

    private boolean shouldSkipFile(File javaFile, AppConfig appConfig) {
        String absolutePath = javaFile.getAbsolutePath();
        List<String> includePaths = appConfig.getIncludePaths();
        List<String> excludePaths = appConfig.getExcludePaths();

        // Check if the file should be excluded
        boolean shouldExclude = excludePaths.stream().anyMatch(absolutePath::contains);
        if (shouldExclude) {
            return true;
        }

        // Check if the file should be included
        return !includePaths.isEmpty() && includePaths.stream().noneMatch(absolutePath::contains);
    }
}
