# 🚀 Dynamic Progress Messaging System - Developer Guide

## ✅ **SYSTEM STATUS: FULLY OPERATIONAL**

**Session ID coordination issue RESOLVED!** ✅ Frontend and backend now use the same session ID, ensuring all progress updates flow correctly to the UI in real-time.

## Quick Start: Adding New Progress Messages

### 📋 **Scenario**: You want to add a new message during processing

**Example**: During file analysis, you want to show "Checking dependencies" at 30% progress

### **Step 1: In your service method**

```java
@Autowired
private UniversalProgressService progressService;

public void yourProcessingMethod(String sessionId) {
    // Simple info message
    progressService.info(sessionId, 30.0, "Checking dependencies");
    
    // Warning message with custom category
    progressService.warning(sessionId, 45.0, "Some dependencies missing");
    
    // Success message
    progressService.success(sessionId, 100.0, "All dependencies resolved!");
    
    // Custom message with additional data
    Map<String, Object> extraData = Map.of(
        "dependenciesChecked", 25,
        "dependenciesMissing", 3
    );
    progressService.customWithData(sessionId, 30.0, "Dependency analysis complete", 
        ProgressUpdate.ProgressType.ANALYSIS, 
        ProgressUpdate.MessageSeverity.INFO,
        "DEPENDENCY_CHECK", 
        extraData);
}
```

### **That's it!** ✅

The frontend will automatically:
- Update the progress bar to 30%
- Show "Checking dependencies" in the progress log
- Style it appropriately based on severity
- Handle any new message type you create

## 🎨 **Available Message Types & Styling**

### **Quick Methods (Recommended)**
```java
progressService.info(sessionId, progress, "Your message");      // Blue styling
progressService.success(sessionId, progress, "Success!");       // Green styling  
progressService.warning(sessionId, progress, "Warning!");       // Yellow styling
progressService.error(sessionId, progress, "Error occurred");   // Red styling
progressService.debug(sessionId, progress, "Debug info");       // Gray styling (only in debug mode)

// Specific progress types
progressService.analysis(sessionId, progress, "Analyzing...");
progressService.testGeneration(sessionId, progress, "Generating tests...");
progressService.validation(sessionId, progress, "Validating...");
```

### **Advanced Usage**
```java
// Completely custom message
progressService.custom(sessionId, 50.0, "Custom operation", 
    ProgressUpdate.ProgressType.CUSTOM, 
    ProgressUpdate.MessageSeverity.WARNING);

// With additional data and category
progressService.customWithData(sessionId, 75.0, "Processing batch", 
    ProgressUpdate.ProgressType.ANALYSIS,
    ProgressUpdate.MessageSeverity.INFO,
    "BATCH_PROCESSING",
    Map.of("batchSize", 10, "processed", 7));
```

## 🔧 **Adding New Progress Types**

### **Step 1: Add to ProgressType enum** (optional)
```java
// In ProgressUpdate.java
public enum ProgressType {
    INITIALIZATION,
    ANALYSIS,
    TEST_GENERATION,
    VALIDATION,
    COMPLETION,
    ERROR,
    WARNING,
    INFO,
    DEBUG,
    CUSTOM,
    YOUR_NEW_TYPE // <-- Add here
}
```

### **Step 2: Add convenience method** (optional)
```java
// In UniversalProgressService.java
public void yourNewType(String sessionId, Double progress, String message) {
    sendProgress(sessionId, progress, message, ProgressUpdate.ProgressType.YOUR_NEW_TYPE, 
                ProgressUpdate.MessageSeverity.INFO);
}
```

### **Step 3: Use it**
```java
progressService.yourNewType(sessionId, 40.0, "Doing your new operation");
```

## 🎯 **Real-World Examples**

### **Example 1: File Processing with Details**
```java
public void processFiles(String sessionId, List<String> files) {
    progressService.info(sessionId, 0.0, "Starting file processing");
    
    for (int i = 0; i < files.size(); i++) {
        String file = files.get(i);
        double progress = (i * 100.0) / files.size();
        
        progressService.info(sessionId, progress, 
            String.format("Processing %s (%d/%d)", file, i+1, files.size()));
        
        try {
            // Your processing logic
            processFile(file);
        } catch (Exception e) {
            progressService.warning(sessionId, progress, 
                String.format("Failed to process %s: %s", file, e.getMessage()));
        }
    }
    
    progressService.success(sessionId, 100.0, "All files processed successfully!");
}
```

### **Example 2: Multi-Stage Process**
```java
public void complexAnalysis(String sessionId) {
    // Stage 1: Preparation
    progressService.analysis(sessionId, 10.0, "Preparing analysis environment");
    
    // Stage 2: Data collection
    progressService.info(sessionId, 30.0, "Collecting data from repository", "DATA_COLLECTION");
    
    // Stage 3: Processing
    progressService.testGeneration(sessionId, 60.0, "Generating test cases");
    
    // Stage 4: Validation
    progressService.validation(sessionId, 90.0, "Validating generated tests");
    
    // Stage 5: Completion
    progressService.success(sessionId, 100.0, "Analysis completed successfully!");
}
```

### **Example 3: Error Handling**
```java
public void riskyOperation(String sessionId) {
    try {
        progressService.info(sessionId, 25.0, "Starting risky operation");
        
        // Your risky code here
        performRiskyTask();
        
        progressService.success(sessionId, 100.0, "Operation completed without issues");
        
    } catch (MinorException e) {
        progressService.warning(sessionId, 75.0, 
            "Operation completed with warnings: " + e.getMessage());
    } catch (MajorException e) {
        progressService.error(sessionId, 0.0, 
            "Operation failed: " + e.getMessage());
    }
}
```

## 🔍 **Debug Mode**

Users can enable debug mode in the UI to see debug messages:
```java
progressService.debug(sessionId, 45.0, "Cache hit ratio: 85%");
progressService.debug(sessionId, 67.0, "Memory usage: 512MB");
```

These only appear when the user clicks "Debug: ON" in the UI.

## 🎨 **Frontend Automatically Handles**

- ✅ Any new ProgressType you add
- ✅ All severity levels (styling)
- ✅ Progress bar updates
- ✅ Message categorization
- ✅ Debug message filtering
- ✅ Completion detection
- ✅ Additional data logging

**No frontend changes needed for new message types!** 🎉

## 📊 **Performance Tips**

1. **Don't spam messages**: Limit to meaningful progress increments
2. **Use appropriate severity**: Don't use ERROR for warnings
3. **Include progress values**: Always provide meaningful progress percentages
4. **Be descriptive**: Messages should be clear and actionable

## 🔄 **Migration from Old System**

**Old way:**
```java
sendProgressUpdate(sessionId, 50.0, "Processing files", ProgressUpdate.ProgressType.ANALYSIS);
```

**New way:**
```java
progressService.analysis(sessionId, 50.0, "Processing files");
```

**Benefits:**
- ✅ Much cleaner code
- ✅ Better type safety
- ✅ Automatic frontend handling
- ✅ Consistent styling
- ✅ Debug mode support
- ✅ Additional data support
