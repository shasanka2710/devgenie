package com.yourorg.javadoc_generator_ai.controller;

import com.yourorg.javadoc_generator_ai.model.ClassDetails;
import com.yourorg.javadoc_generator_ai.model.PackageDetails;
import com.yourorg.javadoc_generator_ai.parser.JavaCodeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RequestMapping("/docs")
@Controller
public class DocumentationController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentationController.class);
    @Autowired
    private JavaCodeParser javaCodeParser;

    @GetMapping
    public String getPackages(Model model) {
        List<PackageDetails> packages = javaCodeParser.parsePackages();
        model.addAttribute("packages", packages);
        return "index";
    }

    @GetMapping("/package/{packageName}")
    public String getPackageDetails(@PathVariable String packageName, Model model) {
        List<ClassDetails> classes = javaCodeParser.parseClasses(packageName);
        model.addAttribute("classes", classes);
        model.addAttribute("title", "Classes in " + packageName);
        model.addAttribute("description", "List of classes in the package " + packageName);
        model.addAttribute("packagePath", packageName);
        return "package-details";
    }

    @GetMapping("/package/{packagePath}/class/{className}")
    public String getClassDetails(@PathVariable String packagePath, @PathVariable String className, Model model) {
        try {
            File javaFile = new File("src/main/java/" + packagePath.replace(".","/") + "/" + className + ".java");
            ClassDetails classDetails = javaCodeParser.parseClassDetails(javaFile);

            model.addAttribute("fields", classDetails.getFields());
            model.addAttribute("constructors", classDetails.getConstructors());
            model.addAttribute("methods", classDetails.getMethods());
            model.addAttribute("title", "Details for " + className);
            model.addAttribute("description", "Here are the details for " + className);

        } catch (IOException e) {
            model.addAttribute("message", "Failed to parse class details: " + e.getMessage());
        }

        return "class-details";
    }
}

