package com.org.devgenie.controller;

import com.org.devgenie.parser.JavaCodeParser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for handling call graph generation and file uploads.
 *
 * Fix explanation:
 * - To address critical Sonar issues (duplicated string literals), constants INDEX_VIEW and MESSAGE_ATTR were introduced.
 * - This avoids repeating the strings "index" and "message" throughout the code, improving maintainability and reducing the risk of typos.
 * - Using constants for repeated literals is a best practice and required by SonarQube for code quality.
 */
@Controller
public class CallGraphController {
    private static final String INDEX_VIEW = "index"; // Used for view name
    private static final String MESSAGE_ATTR = "message"; // Used for model attribute

    private final JavaCodeParser javaCodeParser;

    public CallGraphController(JavaCodeParser javaCodeParser) {
        this.javaCodeParser = javaCodeParser;
    }

    @GetMapping("/index")
    public String index() {
        return INDEX_VIEW;
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute(MESSAGE_ATTR, "Please select a file to upload.");
            return INDEX_VIEW;
        }
        try {
            // Save the file locally
            String fileName = file.getOriginalFilename();
            // Handle potential null from getOriginalFilename
            if (fileName == null || fileName.isEmpty()) {
                model.addAttribute(MESSAGE_ATTR, "File name is missing or invalid.");
                return INDEX_VIEW;
            }
            Path path = Paths.get("uploads/" + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
            // Generate call graph
            File javaFile = path.toFile();
            javaCodeParser.parseAndGenerateDocs(javaFile);
            // Read the generated call graph
            String callGraphPath = "call-graph/" + fileName.replace(".java", ".txt");
            String callGraph = new String(Files.readAllBytes(Paths.get(callGraphPath)));
            model.addAttribute("callGraph", callGraph);
            model.addAttribute(MESSAGE_ATTR, "File uploaded successfully: " + fileName);
        } catch (IOException e) {
            model.addAttribute(MESSAGE_ATTR, "Failed to upload file: " + e.getMessage());
        }
        return INDEX_VIEW;
    }
}