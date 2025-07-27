package com.org.devgenie.model.coverage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Defines the strategy for test generation based on class characteristics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TestGenerationStrategy {
    
    public enum Strategy {
        /**
         * Generate complete test file directly from AI for small/simple classes
         */
        DIRECT_FULL_FILE,
        
        /**
         * Generate tests in batches and assemble manually for large/complex classes
         */
        BATCH_METHOD_BASED,
        
        /**
         * Merge new test methods with existing test file
         */
        MERGE_WITH_EXISTING
    }
    
    private Strategy strategy;
    private String reasoning;
    private int estimatedTokens;
    private int estimatedLines;
    private boolean hasExistingTestFile;
    private String classComplexity;
    private boolean isSpringBootApplication;
    private int maxTestsPerBatch;
    private boolean requiresValidation;
    
    /**
     * Factory method to determine the best strategy for a given class analysis
     * Works seamlessly for all Java applications: Spring Boot, plain Java, Maven/Gradle projects, etc.
     */
    public static TestGenerationStrategy determine(FileAnalysisResult analysis, boolean existingTestFile, String fileContent) {
        log.info("Determining test generation strategy for class: {}", analysis.getFilePath());
        TestGenerationStrategyBuilder builder = TestGenerationStrategy.builder()
            .hasExistingTestFile(existingTestFile)
            .classComplexity(analysis.getComplexity())
            .requiresValidation(true);
        
        // Analyze class characteristics for any Java application
        ClassCharacteristics characteristics = analyzeClassCharacteristics(fileContent);
        builder.isSpringBootApplication(characteristics.isSpringBootApp);
        
        // Estimate file size and complexity
        int estimatedLines = fileContent.split("\n").length;
        int estimatedTokens = estimateTokens(fileContent);
        log.info("Estimated lines: {}, Estimated tokens: {}", estimatedLines, estimatedTokens);
        
        builder.estimatedLines(estimatedLines)
               .estimatedTokens(estimatedTokens);
        
        // Decision logic for strategy selection
        if (existingTestFile) {
            return builder.strategy(Strategy.MERGE_WITH_EXISTING)
                         .reasoning("Existing test file detected - merging new tests with existing content")
                         .maxTestsPerBatch(calculateOptimalBatchSize(estimatedTokens, analysis.getComplexity()))
                         .build();
        }
        
        // For simple classes - use direct approach (regardless of framework)
        if (shouldUseDirectGeneration(estimatedLines, estimatedTokens, analysis.getComplexity(), characteristics)) {
            return builder.strategy(Strategy.DIRECT_FULL_FILE)
                         .reasoning(buildDirectGenerationReasoning(characteristics, estimatedLines, estimatedTokens))
                         .build();
        }
        
        // For complex/large classes - use batch approach
        return builder.strategy(Strategy.BATCH_METHOD_BASED)
                     .reasoning(buildBatchGenerationReasoning(characteristics, estimatedLines, estimatedTokens, analysis.getComplexity()))
                     .maxTestsPerBatch(calculateOptimalBatchSize(estimatedTokens, analysis.getComplexity()))
                     .build();
    }
    
    /**
     * Analyze class characteristics for any type of Java application
     * Works with Spring Boot, plain Java, Jakarta EE, Micronaut, Quarkus, Maven/Gradle projects, etc.
     */
    private static ClassCharacteristics analyzeClassCharacteristics(String fileContent) {
        ClassCharacteristics characteristics = new ClassCharacteristics();
        
        // Framework detection - Spring
        characteristics.isSpringBootApp = fileContent.contains("@SpringBootApplication") || 
                                         (fileContent.contains("SpringApplication.run") && fileContent.contains("public static void main"));
        characteristics.isSpringComponent = fileContent.contains("@Component") || fileContent.contains("@Service") || 
                                           fileContent.contains("@Repository") || fileContent.contains("@Controller") ||
                                           fileContent.contains("@RestController") || fileContent.contains("@Configuration");
        
        // Framework detection - Other frameworks
        characteristics.isJakartaEE = fileContent.contains("javax.") || fileContent.contains("jakarta.");
        characteristics.isMicronautApp = fileContent.contains("@MicronautTest") || fileContent.contains("io.micronaut");
        characteristics.isQuarkusApp = fileContent.contains("@QuarkusTest") || fileContent.contains("io.quarkus");
        characteristics.isJunitTest = fileContent.contains("@Test") || fileContent.contains("junit");
        
        // Class type detection
        characteristics.isMainClass = fileContent.contains("public static void main");
        characteristics.isUtilityClass = isUtilityClass(fileContent);
        characteristics.isDataClass = isDataClass(fileContent);
        characteristics.isInterface = fileContent.contains("public interface") || fileContent.contains("interface");
        characteristics.isAbstractClass = fileContent.contains("abstract class");
        characteristics.isEnum = fileContent.contains("public enum") || fileContent.contains("enum");
        
        // Servlet/Web related
        characteristics.isServlet = fileContent.contains("extends HttpServlet") || fileContent.contains("@WebServlet");
        characteristics.isFilter = fileContent.contains("implements Filter") || fileContent.contains("@WebFilter");
        characteristics.isListener = fileContent.contains("implements ServletContextListener");
        
        // Persistence related
        characteristics.isJpaEntity = fileContent.contains("@Entity") || fileContent.contains("@Table");
        characteristics.isRepository = fileContent.contains("Repository") || fileContent.contains("@Repository");
        
        // Testing related
        characteristics.isTestClass = fileContent.contains("Test") && (fileContent.contains("@Test") || 
                                     fileContent.contains("@BeforeEach") || fileContent.contains("@AfterEach"));
        
        // Complexity indicators
        characteristics.hasComplexLogic = fileContent.contains("if") && fileContent.contains("else") && fileContent.contains("for");
        characteristics.hasExceptionHandling = fileContent.contains("try") || fileContent.contains("catch") || fileContent.contains("throws");
        characteristics.hasAnnotations = countAnnotations(fileContent) > 2;
        characteristics.methodCount = countMethods(fileContent);
        characteristics.hasInheritance = fileContent.contains("extends") || fileContent.contains("implements");
        characteristics.hasGenerics = fileContent.contains("<") && fileContent.contains(">");
        
        return characteristics;
    }
    
    /**
     * Enhanced utility class detection for any Java project
     */
    private static boolean isUtilityClass(String fileContent) {
        // Classic utility class patterns
        boolean hasOnlyStaticMethods = fileContent.contains("static") && !fileContent.contains("@Component") && 
                                      !fileContent.contains("@Service") && !fileContent.contains("@Repository");
        
        // Common utility class naming patterns
        boolean hasUtilityNaming = fileContent.contains("class") && 
                                  (fileContent.contains("Util") || fileContent.contains("Helper") || 
                                   fileContent.contains("Constants") || fileContent.contains("Tools"));
        
        // Private constructor pattern (common in utility classes)
        boolean hasPrivateConstructor = fileContent.contains("private") && fileContent.contains("()");
        
        // Final class without instance methods
        boolean isFinalClassWithStaticOnly = fileContent.contains("final class") && 
                                            fileContent.contains("static") && 
                                            !fileContent.contains("public void ") && 
                                            !fileContent.contains("protected void ");
        
        return hasOnlyStaticMethods && (hasUtilityNaming || hasPrivateConstructor || isFinalClassWithStaticOnly);
    }
    
    /**
     * Enhanced data class detection for any Java project
     */
    private static boolean isDataClass(String fileContent) {
        // JPA/Hibernate entities
        boolean isJpaEntity = fileContent.contains("@Entity") || fileContent.contains("@Table");
        
        // Lombok data classes
        boolean isLombokData = fileContent.contains("@Data") || 
                              (fileContent.contains("@Getter") && fileContent.contains("@Setter"));
        
        // Record classes (Java 14+)
        boolean isRecord = fileContent.contains("public record") || fileContent.contains("record");
        
        // Simple POJO pattern - mostly getters/setters
        boolean isPojo = fileContent.contains("public class") && countMethods(fileContent) < 10 &&
                        (fileContent.contains("get") || fileContent.contains("set")) &&
                        !fileContent.contains("@Service") && !fileContent.contains("@Component");
        
        // DTO/VO pattern
        boolean isDtoVo = fileContent.contains("class") && 
                         (fileContent.contains("DTO") || fileContent.contains("VO") || 
                          fileContent.contains("Dto") || fileContent.contains("ValueObject"));
        
        return isJpaEntity || isLombokData || isRecord || isPojo || isDtoVo;
    }
    
    /**
     * Determine if direct generation should be used based on multiple factors for any Java application
     */
    private static boolean shouldUseDirectGeneration(int estimatedLines, int estimatedTokens, String complexity, ClassCharacteristics characteristics) {
        // Always use direct for very simple cases
        if (estimatedLines < 50 && estimatedTokens < 1000) {
            return true;
        }
        
        // Use direct for main classes (any framework)
        if (characteristics.isMainClass) {
            return true;
        }
        
        // Use direct for utility classes, enums, and simple data classes
        if (characteristics.isUtilityClass || characteristics.isEnum || 
            (characteristics.isDataClass && estimatedLines < 150)) {
            return true;
        }
        
        // Use direct for interfaces and abstract classes (simpler test requirements)
        if (characteristics.isInterface || characteristics.isAbstractClass) {
            return true;
        }
        
        // Use direct for test classes themselves (testing test code)
        if (characteristics.isTestClass) {
            return true;
        }
        
        // Use direct for simple web components (servlets, filters, listeners)
        if ((characteristics.isServlet || characteristics.isFilter || characteristics.isListener) && 
            estimatedLines < 100) {
            return true;
        }
        
        // Use direct for simple JPA entities and DTOs
        if ((characteristics.isJpaEntity || characteristics.isDataClass) && 
            estimatedLines < 120 && characteristics.methodCount <= 8) {
            return true;
        }
        
        // Use direct for small-medium classes with low-medium complexity (any framework)
        if (estimatedLines < 120 && estimatedTokens < 2500 && 
            ("LOW".equals(complexity) || "MEDIUM".equals(complexity))) {
            return true;
        }
        
        // Use direct for classes with few methods regardless of lines (simple structure)
        if (characteristics.methodCount <= 5 && estimatedTokens < 3000) {
            return true;
        }
        
        // Special handling for framework-specific simple classes
        if (characteristics.isSpringComponent && estimatedLines < 100 && 
            characteristics.methodCount <= 6 && !characteristics.hasComplexLogic) {
            return true;
        }
        
        if ((characteristics.isMicronautApp || characteristics.isQuarkusApp) && 
            estimatedLines < 80 && characteristics.methodCount <= 5) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Build reasoning for direct generation strategy (framework-agnostic)
     */
    private static String buildDirectGenerationReasoning(ClassCharacteristics characteristics, int estimatedLines, int estimatedTokens) {
        StringBuilder reasoning = new StringBuilder();
        
        if (characteristics.isMainClass) {
            if (characteristics.isSpringBootApp) {
                reasoning.append("Spring Boot main application class detected - simple direct generation suitable");
            } else if (characteristics.isMicronautApp) {
                reasoning.append("Micronaut main application class detected - direct generation appropriate");
            } else if (characteristics.isQuarkusApp) {
                reasoning.append("Quarkus main application class detected - direct generation suitable");
            } else {
                reasoning.append("Main class detected - simple direct generation suitable");
            }
        } else if (characteristics.isUtilityClass) {
            reasoning.append("Utility class detected - direct generation for static method testing");
        } else if (characteristics.isEnum) {
            reasoning.append("Enum class detected - simple direct generation appropriate");
        } else if (characteristics.isInterface || characteristics.isAbstractClass) {
            reasoning.append("Interface/Abstract class detected - direct generation for contract testing");
        } else if (characteristics.isDataClass) {
            if (characteristics.isJpaEntity) {
                reasoning.append("JPA Entity class detected - direct generation for entity testing");
            } else {
                reasoning.append("Data class detected - direct generation for simple POJO testing");
            }
        } else if (characteristics.isServlet) {
            reasoning.append("Servlet class detected - direct generation for web component testing");
        } else if (characteristics.isFilter) {
            reasoning.append("Filter class detected - direct generation for filter testing");
        } else if (characteristics.isListener) {
            reasoning.append("Listener class detected - direct generation for listener testing");
        } else if (characteristics.isTestClass) {
            reasoning.append("Test class detected - direct generation for test code validation");
        } else if (estimatedLines < 50) {
            reasoning.append("Very small class - direct generation most efficient");
        } else if (characteristics.isSpringComponent) {
            reasoning.append("Spring component class - direct generation suitable for simple testing");
        } else {
            reasoning.append("Small/medium class suitable for direct full-file generation");
        }
        
        reasoning.append(" (").append(estimatedLines).append(" lines, ~").append(estimatedTokens).append(" tokens)");
        return reasoning.toString();
    }
    
    /**
     * Build reasoning for batch generation strategy (framework-agnostic)
     */
    private static String buildBatchGenerationReasoning(ClassCharacteristics characteristics, int estimatedLines, int estimatedTokens, String complexity) {
        StringBuilder reasoning = new StringBuilder();
        
        if ("HIGH".equals(complexity) || "VERY_HIGH".equals(complexity)) {
            reasoning.append("High complexity class requires batch-based generation for better test coverage");
        } else if (estimatedLines > 200) {
            reasoning.append("Large class requires batch-based generation for manageable test creation");
        } else if (estimatedTokens > 4000) {
            reasoning.append("Large token count requires batch approach to avoid AI limitations");
        } else if (characteristics.hasComplexLogic) {
            reasoning.append("Complex business logic requires methodical batch-based test generation");
        } else if (characteristics.isRepository && characteristics.methodCount > 8) {
            reasoning.append("Complex repository class requires batch approach for comprehensive data layer testing");
        } else if (characteristics.isSpringComponent && characteristics.hasInheritance) {
            reasoning.append("Complex Spring component with inheritance requires batch-based generation");
        } else if (characteristics.hasGenerics && characteristics.methodCount > 6) {
            reasoning.append("Generic class with multiple methods requires batch approach for type-safe testing");
        } else {
            reasoning.append("Class characteristics require batch-based generation for optimal test quality");
        }
        
        reasoning.append(" (").append(estimatedLines).append(" lines, ~").append(estimatedTokens).append(" tokens, ")
                .append(complexity).append(" complexity)");
        return reasoning.toString();
    }
    
    /**
     * Count number of methods in the class (rough estimation)
     */
    private static int countMethods(String fileContent) {
        int count = 0;
        String[] lines = fileContent.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if ((trimmed.contains("public ") || trimmed.contains("private ") || trimmed.contains("protected ")) &&
                trimmed.contains("(") && trimmed.contains(")") && 
                !trimmed.contains("class ") && !trimmed.contains("interface ") && !trimmed.contains("=")) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Count number of annotations in the class
     */
    private static int countAnnotations(String fileContent) {
        return (int) fileContent.lines().filter(line -> line.trim().startsWith("@")).count();
    }
    
    /**
     * Inner class to hold class characteristics for any Java application type
     */
    private static class ClassCharacteristics {
        // Framework detection
        boolean isSpringBootApp = false;
        boolean isSpringComponent = false;
        boolean isJakartaEE = false;
        boolean isMicronautApp = false;
        boolean isQuarkusApp = false;
        boolean isJunitTest = false;
        
        // Class type detection
        boolean isMainClass = false;
        boolean isUtilityClass = false;
        boolean isDataClass = false;
        boolean isInterface = false;
        boolean isAbstractClass = false;
        boolean isEnum = false;
        
        // Web/Servlet related
        boolean isServlet = false;
        boolean isFilter = false;
        boolean isListener = false;
        
        // Persistence related
        boolean isJpaEntity = false;
        boolean isRepository = false;
        
        // Testing related
        boolean isTestClass = false;
        
        // Complexity indicators
        boolean hasComplexLogic = false;
        boolean hasExceptionHandling = false;
        boolean hasAnnotations = false;
        boolean hasInheritance = false;
        boolean hasGenerics = false;
        int methodCount = 0;
    }
    
    /**
     * Rough estimation of tokens based on file content
     */
    private static int estimateTokens(String content) {
        // Rough estimation: ~4 characters per token on average for code
        return content.length() / 4;
    }
    
    /**
     * Calculate optimal batch size based on tokens and complexity
     */
    private static int calculateOptimalBatchSize(int estimatedTokens, String complexity) {
        if ("HIGH".equals(complexity) || "VERY_HIGH".equals(complexity)) {
            return 3; // Smaller batches for complex classes
        }
        if (estimatedTokens > 4000) {
            return 4; // Medium batches for large files
        }
        return 5; // Default batch size
    }
}
