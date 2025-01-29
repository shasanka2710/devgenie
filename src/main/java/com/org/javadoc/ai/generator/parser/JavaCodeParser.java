package com.org.javadoc.ai.generator.parser;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.org.javadoc.ai.generator.ai.SpringAiCommentGenerator;
import com.org.javadoc.ai.generator.config.AppConfig;
import com.org.javadoc.ai.generator.model.ClassDetails;
import com.org.javadoc.ai.generator.model.MethodDetails;
import com.org.javadoc.ai.generator.model.PackageDetails;
import com.org.javadoc.ai.generator.util.PathConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class JavaCodeParser {

    private static final Logger logger = LoggerFactory.getLogger(JavaCodeParser.class);

    @Autowired
    private SpringAiCommentGenerator aiCommentGenerator;

    @Autowired
    private AppConfig appConfig;

    public JavaCodeParser() {
        //Set symbol solver
        setSymbolSolver();
    }


    public void parseAndGenerateDocs(File javaFile) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        Optional<TypeDeclaration<?>> typeDeclaration = cu.getPrimaryType();
        if (!typeDeclaration.isPresent()) {
            logger.warn("No primary type found in file: " + javaFile.getName());
            return;
        }
        String className = typeDeclaration.get().getNameAsString();
        //Class level Java documentation
        //  Javadoc classJavadoc = createOrUpdateClassJavadoc(typeDeclaration.get(), className);
        // typeDeclaration.get().setJavadocComment(classJavadoc);
        //Method Iteration
        for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
            //Identifying cyclomatic complexity
            /*int complexity = calculateCyclomaticComplexity(method);
            if (complexity > appConfig.getCyclomaticComplexityThreshold()) {
                logger.warn("Method " + method.getNameAsString() + " in class " + className + " has cyclomatic complexity " + complexity);
            }*/
            //method level java documentation
            Javadoc javadoc = createOrUpdateMethodDoc(method, className);
            method.setJavadocComment(javadoc);
            // Generate call graph
            // generateCallGraph(method, javaFile);
        }
        // Save the modified CompilationUnit back to the file
        if (!appConfig.isDryRun()) {
            Files.write(javaFile.toPath(), cu.toString().getBytes());
        }
    }

    private static void setSymbolSolver() {
        // Create a CombinedTypeSolver and add the ReflectionTypeSolver
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java")));
        // Add the ClassLoaderTypeSolver to include all JAR files from the classpath
        combinedTypeSolver.add(new ClassLoaderTypeSolver(Thread.currentThread().getContextClassLoader()));
        // Create a JavaSymbolSolver with the CombinedTypeSolver
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        //
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }


    private Javadoc createOrUpdateClassJavadoc(TypeDeclaration<?> typeDeclaration, String className) {
        Javadoc javadoc = typeDeclaration.getJavadoc().orElse(new Javadoc(new JavadocDescription()));
        // Update main description for the class
        if (javadoc.getDescription().isEmpty()) {
            String classDescription = (appConfig.isEnableAi() && aiCommentGenerator != null) ? aiCommentGenerator.generateClassComment(typeDeclaration.toString(), className) : "TODO: Add class description here.";
            javadoc = new Javadoc(JavadocDescription.parseText(classDescription));
        }
        return javadoc;
    }


    private Javadoc createOrUpdateMethodDoc(MethodDeclaration method, String className) {
        Javadoc javadoc = method.getJavadoc().orElse(new Javadoc(new JavadocDescription()));
        // Update main description
        if (javadoc.getDescription().isEmpty()) {
            String methodCode = method.toString();
            String aiComment = (appConfig.isEnableAi() && aiCommentGenerator != null) ? aiCommentGenerator.generateMethodComment(methodCode, className) : "TODO: Add method description here.";
            javadoc = new Javadoc(JavadocDescription.parseText(aiComment));
        }
        // Update or add parameter descriptions
        Javadoc finalJavadoc = javadoc;
        if (appConfig.isEnableAi() && aiCommentGenerator != null) {
            methodParameterAndReturnDocGen(method, finalJavadoc, javadoc);
        }
        return finalJavadoc;
    }


    private static void methodParameterAndReturnDocGen(MethodDeclaration method, Javadoc finalJavadoc, Javadoc javadoc) {
        method.getParameters().forEach(parameter -> {
            String paramName = parameter.getNameAsString();
            Optional<JavadocBlockTag> existingTag = finalJavadoc.getBlockTags().stream().filter(tag -> tag.getType().equals(JavadocBlockTag.Type.PARAM) && tag.getName().equals(paramName)).findFirst();
            if (!existingTag.isPresent()) {
                finalJavadoc.addBlockTag("param", paramName, "TODO: Add parameter description.");
            }
        });
        // Update or add return description
        if (!method.getType().isVoidType()) {
            Optional<JavadocBlockTag> returnTag = javadoc.getBlockTags().stream().filter(tag -> tag.getType().equals(JavadocBlockTag.Type.RETURN)).findFirst();
            if (!returnTag.isPresent()) {
                javadoc.addBlockTag("return", "TODO: Add return value description.");
            }
        }
        // Update or add throws description
        method.getThrownExceptions().forEach(thrownException -> {
            String exceptionName = thrownException.asString();
            Optional<JavadocBlockTag> throwsTag = finalJavadoc.getBlockTags().stream().filter(tag -> tag.getType().equals(JavadocBlockTag.Type.THROWS) && tag.getName().equals(exceptionName)).findFirst();
            if (!throwsTag.isPresent()) {
                finalJavadoc.addBlockTag("throws", exceptionName, "TODO: Add exception description.");
            }
        });
    }

    private int calculateCyclomaticComplexity(MethodDeclaration method) {
        // Start with 1 for the method itself
        int complexity = 1;
        for (Statement stmt : method.getBody().orElseThrow().getStatements()) {
            if (stmt instanceof IfStmt || stmt instanceof ForStmt || stmt instanceof WhileStmt || stmt instanceof DoStmt || stmt instanceof SwitchStmt || stmt instanceof TryStmt) {
                complexity++;
            }
        }
        return complexity;
    }


    private void generateCallGraph(MethodDeclaration method, File javaFile) throws IOException {
        String methodName = method.getNameAsString();
        String relativePath = javaFile.getPath().replaceFirst("uploads", "call-graph");
        Path outputPath = Paths.get(relativePath).getParent();
        if (outputPath != null) {
            Files.createDirectories(outputPath);
        }
        Path callGraphFile = outputPath.resolve(methodName + ".txt");
        String callGraph = buildCallGraph(method, 0, appConfig.getCallGraphDepth());
        Files.write(callGraphFile, callGraph.getBytes());
    }


    private String buildCallGraph(MethodDeclaration method, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth) {
            return "";
        }
        StringBuilder callGraph = new StringBuilder();
        callGraph.append(method.getNameAsString()).append("\n");
        method.findAll(MethodCallExpr.class).forEach(call -> {
            try {
                Optional<MethodDeclaration> calledMethod = call.resolve().toAst().filter(MethodDeclaration.class::isInstance).map(MethodDeclaration.class::cast);
                calledMethod.ifPresent(m -> callGraph.append("  ".repeat(currentDepth + 1)).append(buildCallGraph(m, currentDepth + 1, maxDepth)));
            } catch (IllegalStateException e) {
                logger.error("Symbol resolution not configured for method call: " + call, e);
            }
        });
        return callGraph.toString();
    }


    public ClassDetails parseClassDetails(File javaFile) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        TypeDeclaration<?> typeDeclaration = cu.getPrimaryType().orElseThrow(() -> new IllegalArgumentException("No primary type found"));
        String className = typeDeclaration.getNameAsString();
        String classDescription = "Description of " + className;
        List<String> fields = typeDeclaration.getFields().stream().map(FieldDeclaration::toString).collect(Collectors.toList());
        List<String> constructors = typeDeclaration.getConstructors().stream().map(ConstructorDeclaration::getNameAsString).collect(Collectors.toList());
        List<MethodDetails> methods = typeDeclaration.getMethods().stream().map(method -> new MethodDetails(method.getDeclarationAsString(), (method.getJavadoc().isPresent() && method.getJavadoc().get().toText()!=null) ? method.getJavadoc().get().toText().toString() : "Description of " + method.getNameAsString(), method.getType().asString(), method.getThrownExceptions().toString())).collect(Collectors.toList());
        return new ClassDetails(className, classDescription, fields, constructors, methods);
    }


    public List<PackageDetails> parsePackages() {
        List<PackageDetails> packages = new ArrayList<>();
        try {
            Files.walk(Paths.get("src/main/java/com/org/javadoc/ai/generator")).filter(Files::isDirectory).forEach(path -> {
                String packageName = path.toString().replace("src/main/java/", "").replace("/", ".");
                packages.add(new PackageDetails(packageName, "Description of " + packageName));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packages;
    }


    public List<ClassDetails> parseClasses(String packageName) {
        List<ClassDetails> classes = new ArrayList<>();
        try {
            Path packagePath = Paths.get("src/main/java/" + packageName.replace(".", "/"));
            Files.list(packagePath).filter(Files::isRegularFile).filter(file -> file.toString().endsWith(".java")).forEach(file -> {
                try {
                    CompilationUnit cu = StaticJavaParser.parse(file);
                    TypeDeclaration<?> typeDeclaration = cu.getPrimaryType().orElseThrow(() -> new IllegalArgumentException("No primary type found"));
                    classes.add(new ClassDetails(typeDeclaration.getNameAsString(), (typeDeclaration.getJavadocComment() != null) ? typeDeclaration.getJavadocComment().toString() : "TODO: Description of " + typeDeclaration.getNameAsString(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    public String identifyFixUsingLLModel(String className, String description) throws FileNotFoundException {
        logger.info("Identifying fix using LL model for class: " + className);
        CompilationUnit cu = getCompilationUnit(className);
        Optional<TypeDeclaration<?>> typeDeclaration = cu.getPrimaryType();
        String classNameFromFile = typeDeclaration.get().getNameAsString();
        String fixedCode = (appConfig.isEnableAi() && aiCommentGenerator != null) ? aiCommentGenerator.fixSonarIssue(classNameFromFile, typeDeclaration.get().getParentNode().get().toString(), description) : typeDeclaration.get().toString();
        logger.info("Original code: " + typeDeclaration.get().getParentNode().get().toString());
        logger.info("Fixed code: " + fixedCode);
        String sanitizedOutput = fixedCode.replaceAll("```[a-zA-Z]*", "").replaceAll("```", "");
        return sanitizedOutput;
    }

    public static CompilationUnit getCompilationUnit(String className) throws FileNotFoundException {
        Path filePath = Paths.get(PathConverter.toSlashedPath(className));

        File file = new File(String.valueOf(filePath));
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        CompilationUnit cu = StaticJavaParser.parse(file);
        return cu;
    }


    public boolean isValidJavaCode(String code) {
        try {
            // Attempt to parse the code
            CompilationUnit compilationUnit = StaticJavaParser.parse(code);
            return true; // Successfully parsed
        } catch (ParseProblemException | IllegalArgumentException e) {
            // Syntax or parsing issues
            System.err.println("Code validation failed: " + e.getMessage());
            return false;
        }
    }
}
