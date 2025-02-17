package com.org.devgenie.controller;

import com.org.devgenie.exception.DocGenerationException;
import com.org.devgenie.service.DocGeneratorService;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;

@RestController
public class AiJavaDocGenerationController {

    private final DocGeneratorService docGeneratorService;

    public AiJavaDocGenerationController(DocGeneratorService docGeneratorService) {
        this.docGeneratorService = docGeneratorService;
    }

    @PostMapping("/generate")
    public String generateDocs(@RequestParam String inputType, @RequestParam String source, @RequestParam(required = false) String configFilePath, @RequestParam(required = false) String gitBranch) throws DocGenerationException {
        try {
            docGeneratorService.generateDocs(inputType, source, configFilePath, gitBranch);
            return "Documentation generated successfully at: " + source;
        } catch (IOException | GitAPIException e) {
            throw new DocGenerationException("Error generating documentation: " + e.getMessage(), e);
        } 
    }
}