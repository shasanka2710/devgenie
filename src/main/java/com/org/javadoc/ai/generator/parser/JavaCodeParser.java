package com.org.javadoc.ai.generator.parser;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.File;
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

    /**
     * This Java code, using the JavaParser library, analyzes a Java source file and potentially modifies it. Here's a breakdown:
     *
     * **Purpose:**
     *
     * The primary purpose of this code is to process Java source code, specifically focusing on class and method level documentation and potentially other code quality metrics.
     *
     * **Key Features:**
     *
     * * **Parsing Java Source Code:**
     *     * `CompilationUnit cu = StaticJavaParser.parse(javaFile);`:  This line reads the Java source code from the provided `javaFile` and parses it into an abstract syntax tree (AST) represented by the `CompilationUnit` object.
     * * **Identifying the Primary Class:**
     *     * `Optional<TypeDeclaration<?>> typeDeclaration = cu.getPrimaryType();`:  It attempts to identify the main class declared within the Java file.
     *     * The code handles cases where a primary class might not be found (e.g., the file might contain only interfaces or enums).
     * * **(Commented Out) Class-Level Javadoc Handling:**
     *     * The commented-out lines suggest the code was intended to:
     *         * Generate or update Javadoc documentation for the class (`createOrUpdateClassJavadoc`).
     *         * Set the generated Javadoc back to the class declaration within the AST.
     * * **Method-Level Analysis and Modification:**
     *     * **Iteration:** The code iterates through all method declarations (`MethodDeclaration`) found within the parsed source code.
     *     * **(Commented Out) Cyclomatic Complexity:**
     *         * The commented-out section indicates the code could calculate the cyclomatic complexity of each method.
     *         * This metric helps assess code complexity and potential maintainability issues.
     *     * **Method-Level Javadoc:**
     *         * Similar to class-level Javadoc, the code generates or updates Javadoc documentation for each method (`createOrUpdateMethodDoc`).
     *         * The generated Javadoc is then set back to the method declaration in the AST.
     *     * **(Commented Out) Call Graph Generation:**
     *         * The commented line suggests the potential for generating a call graph, which visualizes the relationships between method calls in the code.
     * * **Saving Modifications (Conditional):**
     *     * `if (!appConfig.isDryRun()) { ... }`: The code checks a configuration setting (`appConfig.isDryRun()`).
     *     * If not in "dry run" mode, it writes the potentially modified AST back to the original Java source file, overwriting its contents.
     *
     * **In Summary:**
     *
     * This code snippet demonstrates a process for programmatically analyzing and potentially modifying Java source code. While some features are commented out, it highlights capabilities for:
     *
     * * Automated Javadoc generation or updating.
     * * Code complexity analysis (cyclomatic complexity).
     * * Potential for call graph generation.
     *
     * The code acts as a foundation for building more sophisticated code analysis and manipulation tools.
     *
     * @param javaFile TODO: Add parameter description.
     * @throws IOException TODO: Add exception description.
     */
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

    /**
     * This Java code snippet configures a **symbol solver** for **StaticJavaParser**, a library used for analyzing and manipulating Java code. Let's break down its purpose and key features:
     *
     * **Purpose:**
     *
     * * **Resolve Symbols:** In simple terms, this code helps StaticJavaParser understand the meaning of different symbols (like class names, method names, variable names) used in the Java code it's analyzing.
     * * **Accurate Analysis:** Without a symbol solver, StaticJavaParser would have a very limited understanding of the code, making it difficult to perform advanced analysis or modifications.
     *
     * **Key Features:**
     *
     * * **CombinedTypeSolver:** This is the heart of the symbol resolution mechanism. It aggregates information from multiple sources (TypeSolvers) to provide a comprehensive view of the code's structure.
     *     * **ReflectionTypeSolver:** This solver uses Java's reflection capabilities to understand the types and members of classes available at runtime.
     *     * **JavaParserTypeSolver:** This solver directly analyzes the Java source code within the specified directory ("src/main/java" in this case) to understand the relationships between classes and their members.
     *     * **ClassLoaderTypeSolver:** This solver explores the JAR files present in the application's classpath, allowing it to resolve symbols from external libraries.
     * * **JavaSymbolSolver:** This component acts as the bridge between StaticJavaParser and the CombinedTypeSolver. It uses the information gathered by the TypeSolvers to answer StaticJavaParser's queries about symbols.
     * * **StaticJavaParser Configuration:** Finally, the code sets the configured `symbolSolver` instance on the `StaticJavaParser` configuration. This ensures that all subsequent code analysis performed by StaticJavaParser leverages the symbol resolution capabilities.
     *
     * **In essence, this code snippet sets up a powerful symbol resolution mechanism for StaticJavaParser, enabling it to perform more accurate and sophisticated code analysis by understanding the complete context of the Java code it's working with.**
     */
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

    /**
     * This Java code snippet defines a method called `createOrUpdateClassJavadoc`. Let's break down its purpose and key features:
     *
     * **Purpose:**
     *
     * This method is designed to either create a new Javadoc comment for a class declaration or update an existing one. It leverages the power of AI (if enabled) to generate a descriptive comment when one is missing.
     *
     * **Key Features:**
     *
     * * **Javadoc Management:** The core functionality revolves around the `Javadoc` class, which represents documentation comments in Java code.
     * * **TypeDeclaration Input:** It takes a `TypeDeclaration` object as input. This object represents a class or interface declaration in your Java code.
     * * **className Parameter:** The `className` parameter likely provides a more readable name for the class being documented.
     * * **Javadoc Retrieval & Creation:**
     *     * It first attempts to retrieve an existing Javadoc comment associated with the `TypeDeclaration`.
     *     * If no Javadoc is found (using `orElse`), it creates a new, empty `Javadoc` object.
     * * **AI-Powered Comment Generation (Optional):**
     *     * The code checks if AI comment generation is enabled (`appConfig.isEnableAi()`) and if an `aiCommentGenerator` is available.
     *     * If both conditions are met, it delegates class comment generation to the `aiCommentGenerator`.
     * * **Default Comment:**
     *     * If AI generation is not active or if it fails, a placeholder comment "TODO: Add class description here." is used.
     * * **Javadoc Update:** The generated or placeholder comment is used to create a new `Javadoc` object, effectively updating the documentation.
     * * **Javadoc Return:** The method returns the updated (or newly created) `Javadoc` object.
     *
     * **Example Usage:**
     *
     * Let's say you have a class declaration like this:
     *
     * ```java
     * public class MyClass {
     *     // ... class content
     * }
     * ```
     *
     * You could use the method like so:
     *
     * ```java
     * Javadoc updatedJavadoc = createOrUpdateClassJavadoc(myClassDeclaration, "MyClass");
     * ```
     *
     * This would ensure that the `MyClass` declaration has a Javadoc comment, potentially generated by AI if the necessary configurations are in place.
     *
     * @param typeDeclaration TODO: Add parameter description.
     * @param className TODO: Add parameter description.
     * @return TODO: Add return value description.
     */
    private Javadoc createOrUpdateClassJavadoc(TypeDeclaration<?> typeDeclaration, String className) {
        Javadoc javadoc = typeDeclaration.getJavadoc().orElse(new Javadoc(new JavadocDescription()));
        // Update main description for the class
        if (javadoc.getDescription().isEmpty()) {
            String classDescription = (appConfig.isEnableAi() && aiCommentGenerator != null) ? aiCommentGenerator.generateClassComment(typeDeclaration.toString(), className) : "TODO: Add class description here.";
            javadoc = new Javadoc(JavadocDescription.parseText(classDescription));
        }
        return javadoc;
    }

    /**
     * This Java code snippet defines a method called `createOrUpdateMethodDoc`. Its purpose is to **generate or update Javadoc documentation for a given method**.
     *
     * Here's a breakdown of its key features:
     *
     * * **Javadoc Management:**
     *     * **Retrieves Existing Javadoc:** It first tries to retrieve existing Javadoc documentation for the provided `MethodDeclaration` object.
     *     * **Handles Missing Javadoc:** If no Javadoc exists, it creates a new, empty Javadoc object.
     *     * **Updates Existing or Creates New:** The method intelligently updates existing Javadoc content or creates new content if none is present.
     *
     * * **AI-Powered Comment Generation (Optional):**
     *     * **Configuration-Based:** The code uses an `appConfig` object to determine if AI-powered comment generation is enabled.
     *     * **AI Comment Generator:** If enabled, it utilizes an `aiCommentGenerator` (likely an external library or service) to generate method descriptions.
     *     * **Contextual Comment Generation:** The AI comment generator takes the method's code (`methodCode`) and the class name (`className`) as context to produce more relevant comments.
     *
     * * **Method Description Generation/Update:**
     *     * **Checks for Empty Description:** The code specifically checks if the main method description is empty.
     *     * **Generates Description (AI or Default):**
     *         * If AI generation is enabled and the description is empty, it uses the `aiCommentGenerator` to generate a description.
     *         * If AI generation is disabled or unavailable, it inserts a "TODO" placeholder, prompting the developer to add a description manually.
     *
     * * **Parameter and Return Type Documentation:**
     *     * **Delegates to Another Method:** The code calls a separate method `methodParameterAndReturnDocGen` to handle the generation or update of Javadoc comments for method parameters and the return type. This separation likely improves code organization and readability.
     *     * **Conditional AI Generation:**  Similar to the method description, AI-powered generation for parameter and return type comments is also conditional and depends on the `appConfig` and the availability of the `aiCommentGenerator`.
     *
     * * **Returns Updated Javadoc:** The method returns the `finalJavadoc` object, which contains either the updated existing Javadoc or a newly created Javadoc with generated content.
     *
     * **In essence, this code snippet aims to automate the often tedious process of writing Javadoc documentation, especially for new or modified methods. It leverages AI (if enabled) to generate more meaningful descriptions, ultimately promoting better code documentation practices.**
     *
     * @param method TODO: Add parameter description.
     * @param className TODO: Add parameter description.
     * @return TODO: Add return value description.
     */
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

    /**
     * This Java code snippet is designed to automatically generate or update Javadoc documentation for a given method. Let's break down its purpose and key features:
     *
     * **Purpose:**
     *
     * * **Javadoc Automation:** The primary goal is to streamline the process of adding or updating Javadoc comments for methods within your Java code.
     * * **Consistency:** It ensures that essential Javadoc tags like `@param`, `@return`, and `@throws` are present for each method, promoting well-documented code.
     *
     * **Key Features:**
     *
     * * **Parameter Documentation (`@param`)**:
     *     * Iterates through each parameter of the provided `MethodDeclaration`.
     *     * Checks if a `@param` tag already exists for the parameter.
     *     * If not, it adds a `@param` tag with a placeholder description ("TODO: Add parameter description.").
     *
     * * **Return Value Documentation (`@return`)**:
     *     * Verifies if the method is not void (i.e., it returns a value).
     *     * Checks for an existing `@return` tag.
     *     * If absent, adds a `@return` tag with a placeholder description ("TODO: Add return value description.").
     *
     * * **Exception Documentation (`@throws`)**:
     *     * Iterates through any exceptions declared to be thrown by the method.
     *     * Checks for an existing `@throws` tag for each exception type.
     *     * If not found, adds a `@throws` tag with a placeholder description ("TODO: Add exception description.").
     *
     * * **Javadoc Manipulation:** The code utilizes libraries or APIs (likely from a tool like JavaParser) to:
     *     * Parse and represent the `MethodDeclaration`.
     *     * Work with Javadoc elements (`JavadocBlockTag`, `Javadoc`).
     *     * Add new Javadoc tags.
     *
     * **In Essence:** This function acts as a Javadoc helper, ensuring that basic documentation elements are in place for your methods, prompting you to fill in the specific details. This encourages better documentation practices and improves code readability.
     *
     * @param method TODO: Add parameter description.
     * @param finalJavadoc TODO: Add parameter description.
     * @param javadoc TODO: Add parameter description.
     */
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

    /**
     * This Java code defines a method called `calculateCyclomaticComplexity` that calculates the Cyclomatic Complexity of a given Java method. Here's a breakdown:
     *
     * **Purpose:**
     *
     * * **Measure Code Complexity:** Cyclomatic Complexity is a software metric used to indicate the complexity of a program. It essentially quantifies the number of linearly independent paths through a piece of code. Higher complexity suggests code that is more difficult to understand, test, and maintain.
     *
     * **Key Features:**
     *
     * * **Input:** The method takes a `MethodDeclaration` object as input, representing the Java method whose complexity you want to analyze.
     * * **Initialization:** The `complexity` variable is initialized to 1. This accounts for the single path through the method if it has no conditional or looping statements.
     * * **Iterating through Statements:** The code iterates through each `Statement` within the method's body using a `for` loop.
     * * **Identifying Complexity-Increasing Statements:**
     *     * It checks if each statement is one of the following:
     *         * `IfStmt`: If statement
     *         * `ForStmt`: For loop
     *         * `WhileStmt`: While loop
     *         * `DoStmt`: Do-while loop
     *         * `SwitchStmt`: Switch statement
     *         * `TryStmt`: Try-catch block
     *     * For each of these statement types, the `complexity` is incremented by 1. This is because each of these statements introduces a new decision point or branch in the code's execution flow.
     * * **Return Value:** The method returns the calculated `complexity` value as an integer.
     *
     * **Example:**
     *
     * Consider this simple Java method:
     *
     * ```java
     * public void exampleMethod(int x) {
     *     if (x > 10) {
     *         System.out.println("x is greater than 10");
     *     } else {
     *         System.out.println("x is less than or equal to 10");
     *     }
     * }
     * ```
     *
     * The `calculateCyclomaticComplexity` method would return a complexity of **3** for this example:
     *
     * * 1 for the method itself.
     * * 1 for the `if` statement.
     * * 1 for the implicit `else` path within the `if` statement.
     *
     * **Importance:**
     *
     * * **Testability:** Higher Cyclomatic Complexity generally implies a greater need for test cases to achieve good code coverage.
     * * **Maintainability:** Complex code can be difficult to understand and modify, potentially leading to bugs and increased maintenance effort.
     * * **Code Quality Indicator:** Cyclomatic Complexity can be used as a metric to identify areas of your codebase that might benefit from refactoring to improve readability and maintainability.
     *
     * @param method TODO: Add parameter description.
     * @return TODO: Add return value description.
     */
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

    /**
     * This Java code snippet defines a method called `generateCallGraph` that analyzes a given Java method and generates a textual representation of its call graph, saving it to a file. Here's a breakdown of its purpose and key features:
     *
     * **Purpose:**
     *
     * - **Call Graph Generation:** The primary purpose is to create a call graph for a specific Java method. A call graph visually represents the flow of execution within a program, showing which methods are called by other methods. This is valuable for understanding program structure, debugging, and code analysis.
     *
     * **Key Features:**
     *
     * - **Method Input:**
     *     - `method`: A `MethodDeclaration` object representing the Java method to analyze.
     *     - `javaFile`: A `File` object representing the source file of the Java method.
     * - **File Handling:**
     *     - **Output Directory:** It determines the output directory for the call graph file based on the input `javaFile` path, replacing "uploads" with "call-graph" and ensuring the directory structure exists.
     *     - **File Creation:**  Creates a new text file named "[methodName].txt" within the output directory to store the call graph.
     * - **Call Graph Construction:**
     *     - `buildCallGraph(method, 0, appConfig.getCallGraphDepth())`: This is the core logic (not shown in the provided code) that analyzes the input `method` and recursively explores method calls up to a certain depth (controlled by `appConfig.getCallGraphDepth()`) to construct the call graph as a string.
     * - **Writing to File:**
     *     - The generated call graph string is then written to the created output file.
     * - **Error Handling:**
     *     - The method throws an `IOException` to handle potential errors during file system operations.
     *
     * **Example Usage:**
     *
     * Let's say you have a Java method called `calculateSum` in a file located at "uploads/com/example/Calculator.java". This code snippet would:
     *
     * 1. Create an output directory "call-graph/com/example/" if it doesn't exist.
     * 2. Generate a call graph for the `calculateSum` method, analyzing method calls up to a depth specified in `appConfig`.
     * 3. Save the call graph to a file named "calculateSum.txt" in the output directory.
     *
     * **Important Note:** The provided code snippet is incomplete as it relies on the `buildCallGraph` method, which is not defined here. The actual implementation of `buildCallGraph` would involve using Java reflection or parsing libraries to analyze the method's bytecode and identify method calls.
     *
     * @param method TODO: Add parameter description.
     * @param javaFile TODO: Add parameter description.
     * @throws IOException TODO: Add exception description.
     */
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

    /**
     * This Java code defines a recursive function `buildCallGraph` that generates a string representation of the call graph for a given method. Here's a breakdown of its purpose and key features:
     *
     * **Purpose:**
     *
     * - **Call Graph Generation:** The primary purpose of this function is to create a textual representation of the call graph originating from a specific method. A call graph visually depicts the flow of function calls within a program.
     *
     * **Key Features:**
     *
     * - **Recursion:** The function is recursive, meaning it calls itself to handle nested method calls. This allows it to traverse the call graph to a specified depth.
     * - **Depth Control:**
     *     - `currentDepth`: Tracks the current level of recursion (how many levels deep in the call graph we are).
     *     - `maxDepth`: Limits how deep the recursion goes, preventing infinite recursion if there are cycles in the call graph.
     * - **Method Name Extraction:** It retrieves the name of the input `method` using `method.getNameAsString()`.
     * - **Method Call Identification:**
     *     - `method.findAll(MethodCallExpr.class)`: Finds all method call expressions (`MethodCallExpr`) within the body of the input `method`.
     * - **Symbol Resolution (Optional):**
     *     - `call.resolve().toAst()...`: Attempts to resolve each method call expression (`call`) to the actual method declaration (`MethodDeclaration`) it refers to. This might require symbol resolution capabilities in your Java parser/analyzer.
     *     - **Error Handling:** The `try...catch` block handles potential `IllegalStateException` during symbol resolution, logging an error if resolution fails.
     * - **Recursive Call:** If symbol resolution is successful (`calledMethod.ifPresent(...)`), the function recursively calls itself (`buildCallGraph(m, currentDepth + 1, maxDepth)`) for the resolved `calledMethod`, incrementing the `currentDepth`.
     * - **Indentation:**  The `("  ".repeat(currentDepth + 1))` part adds indentation to visually represent the call graph's hierarchical structure.
     * - **String Building:** The `StringBuilder` (`callGraph`) efficiently constructs the final string representation of the call graph.
     *
     * **Example Output:**
     *
     * If the input `method` is `calculateSomething`, and it calls `fetchData` and `processResult`, the output might look like:
     *
     * ```
     * calculateSomething
     *   fetchData
     *     connectToDatabase
     *   processResult
     *     validateData
     * ```
     *
     * **In summary:** This code snippet provides a way to visualize the call relationships between methods in your Java code, which can be extremely useful for understanding code flow, debugging, and refactoring.
     *
     * @param method TODO: Add parameter description.
     * @param currentDepth TODO: Add parameter description.
     * @param maxDepth TODO: Add parameter description.
     * @return TODO: Add return value description.
     */
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

    /**
     * This Java code snippet uses the JavaParser library to analyze a Java source code file and extract key information about a class definition. Here's a breakdown of its purpose and features:
     *
     * **Purpose:**
     *
     * The function `parseClassDetails` takes a Java file as input and returns a `ClassDetails` object containing structured information about the class defined in that file. This information includes:
     *
     * * Class name
     * * Fields (with their declarations)
     * * Constructors (names only)
     * * Methods (with their declarations, Javadoc descriptions, return types, and thrown exceptions)
     *
     * **Key Features:**
     *
     * * **JavaParser Integration:** Leverages the JavaParser library (`StaticJavaParser.parse`) to parse the Java source code into an Abstract Syntax Tree (AST).
     * * **AST Traversal:**  Navigates the AST to extract relevant information:
     *     * `cu.getPrimaryType()` retrieves the main class declaration.
     *     * `typeDeclaration.getNameAsString()`, `.getFields()`, `.getConstructors()`, and `.getMethods()` access specific elements like class name, fields, constructors, and methods.
     * * **Data Extraction:** Extracts data from the AST elements:
     *     * Uses streams and lambda expressions for concise data manipulation.
     *     * Retrieves field declarations, constructor names, method signatures, Javadoc comments (if available), return types, and thrown exceptions.
     * * **Javadoc Handling:**  Attempts to extract Javadoc descriptions for methods. If no Javadoc is found, it provides a default description.
     * * **Custom Object Creation:** Creates a `ClassDetails` object (not shown in the code) to store the extracted information in a structured format.
     *
     * **In summary,** this code snippet demonstrates a practical use case of JavaParser for code analysis. It extracts essential information about a class from its source code, which can be used for various purposes like documentation generation, code analysis tools, or automated code refactoring.
     *
     * @param javaFile TODO: Add parameter description.
     * @return TODO: Add return value description.
     * @throws IOException TODO: Add exception description.
     */
    public ClassDetails parseClassDetails(File javaFile) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        TypeDeclaration<?> typeDeclaration = cu.getPrimaryType().orElseThrow(() -> new IllegalArgumentException("No primary type found"));
        String className = typeDeclaration.getNameAsString();
        String classDescription = "Description of " + className;
        List<String> fields = typeDeclaration.getFields().stream().map(FieldDeclaration::toString).collect(Collectors.toList());
        List<String> constructors = typeDeclaration.getConstructors().stream().map(ConstructorDeclaration::getNameAsString).collect(Collectors.toList());
        List<MethodDetails> methods = typeDeclaration.getMethods().stream().map(method -> new MethodDetails(method.getDeclarationAsString(), (method.getJavadoc() != null) ? method.getJavadoc().get().toText().toString() : "Description of " + method.getNameAsString(), method.getType().asString(), method.getThrownExceptions().toString())).collect(Collectors.toList());
        return new ClassDetails(className, classDescription, fields, constructors, methods);
    }

    /**
     * This Java code snippet defines a method called `parsePackages` that aims to **discover and document Java packages** within a specific directory. Here's a breakdown of its purpose and key features:
     *
     * **Purpose:**
     *
     * - **Package Discovery:** The code automatically finds Java packages located under the directory `src/main/java/com/yourorg/javadoc_generator_ai`.
     * - **Basic Documentation Generation:** It creates simple documentation for each discovered package, consisting of the package name and a generic description.
     *
     * **Key Features:**
     *
     * - **File System Traversal:**
     *     - `Files.walk(Paths.get("..."))` recursively navigates the specified directory and its subdirectories.
     * - **Directory Filtering:**
     *     - `filter(Files::isDirectory)` ensures that only directories (representing Java packages) are processed.
     * - **Package Name Extraction:**
     *     - `path.toString().replace("src/main/java/", "").replace("/", ".")`  transforms the directory path into a valid Java package name (e.g., "com.yourorg.javadoc_generator_ai.subpackage").
     * - **Package Details Storage:**
     *     - `List<PackageDetails> packages = new ArrayList<>();` creates a list to store information about each discovered package.
     *     - `packages.add(new PackageDetails(...))` populates the list with `PackageDetails` objects, each containing a package name and a description.
     * - **Error Handling:**
     *     - `try...catch (IOException e)` block handles potential `IOExceptions` that might occur during file system operations.
     *
     * **In essence, this code snippet provides a basic framework for automatically generating documentation for Java packages within a project.**  You could extend this code to:
     *
     * - Extract more sophisticated documentation from source code comments (e.g., Javadoc).
     * - Generate documentation in various formats (HTML, Markdown, etc.).
     * - Customize the documentation generation process based on specific needs.
     *
     * @return TODO: Add return value description.
     */
    public List<PackageDetails> parsePackages() {
        List<PackageDetails> packages = new ArrayList<>();
        try {
            Files.walk(Paths.get("src/main/java/com/yourorg/javadoc_generator_ai")).filter(Files::isDirectory).forEach(path -> {
                String packageName = path.toString().replace("src/main/java/", "").replace("/", ".");
                packages.add(new PackageDetails(packageName, "Description of " + packageName));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packages;
    }

    /**
     * This Java code snippet is designed to **parse Java source files within a specified package and extract class information**. It uses the **JavaParser** library to achieve this.
     *
     * Here's a breakdown of the code and its key features:
     *
     * **Purpose:**
     *
     * - The primary goal of this code is to analyze Java source code located in a specific package and extract details about the classes defined within those files.
     * - It gathers information such as class names, Javadoc comments, and potentially other details (although the provided code doesn't extract additional information beyond name and Javadoc).
     *
     * **Key Features:**
     *
     * * **Package Scanning:**
     *     - It takes a `packageName` as input, which represents the Java package to scan.
     *     - It constructs the file system path to this package (`src/main/java/` + package name with dots replaced by slashes).
     * * **File Filtering:**
     *     - It uses `Files.list()` to get all files in the package directory.
     *     - It filters the files to only process regular files (not directories) ending with ".java" (Java source files).
     * * **JavaParser Integration:**
     *     - For each Java file:
     *         - It uses `StaticJavaParser.parse(file)` to parse the source code into an Abstract Syntax Tree (AST) representation.
     *         - It retrieves the primary type declaration (which represents a class, interface, enum, etc.) from the AST.
     *         - It extracts the class name and Javadoc comment (if available) from the type declaration.
     * * **ClassDetails Object:**
     *     - It creates a `ClassDetails` object (not shown in the code, but assumed to be defined elsewhere) to store the extracted information (class name, Javadoc).
     *     - It initializes the `ClassDetails` object with the extracted data.
     *     - The code suggests that `ClassDetails` might also store information about fields, methods, and constructors, but these are not extracted in the provided snippet.
     * * **Error Handling:**
     *     - It includes basic error handling using `try-catch` blocks to catch `IOExceptions` that might occur during file system operations or parsing.
     *
     * **Potential Improvements:**
     *
     * - **More Comprehensive Extraction:** The code currently only extracts class names and Javadoc comments. It could be extended to extract more details like:
     *     - Fields and their types
     *     - Methods, their parameters, return types, and Javadoc
     *     - Constructors
     *     - Annotations
     * - **Robust Error Handling:** The error handling could be improved by:
     *     - Providing more informative error messages.
     *     - Potentially handling different exception types separately (e.g., parsing errors vs. file system errors).
     * - **Javadoc Parsing:**  The current code treats Javadoc as plain text. Consider using a dedicated Javadoc parser for more structured extraction of Javadoc tags and content.
     *
     * This code snippet provides a foundation for building a more comprehensive Java code analysis tool. By extending its capabilities and improving its error handling, you can create a powerful utility for understanding and working with Java projects.
     *
     * @param packageName TODO: Add parameter description.
     * @return TODO: Add return value description.
     */
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
}
