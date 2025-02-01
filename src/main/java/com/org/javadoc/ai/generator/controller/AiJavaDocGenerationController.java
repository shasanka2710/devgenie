package com.org.javadoc.ai.generator.controller;

import com.org.javadoc.ai.generator.service.DocGeneratorService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class AiJavaDocGenerationController {

    private final DocGeneratorService docGeneratorService;

    public AiJavaDocGenerationController(DocGeneratorService docGeneratorService) { // Constructor injection
        this.docGeneratorService = docGeneratorService;
    }

    @PostMapping("/generate")
    public String generateDocs(@RequestParam String inputType, @RequestParam String source, @RequestParam(required = false) String configFilePath, @RequestParam(required = false) String gitBranch) {
        try {
            docGeneratorService.generateDocs(inputType, source, configFilePath, gitBranch);
            return "Documentation generated successfully at: " + source;
        } catch (IOException e) {
            return "Error generating documentation: " + e.getMessage();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }
}
