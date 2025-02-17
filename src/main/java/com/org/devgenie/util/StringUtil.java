package com.org.devgenie.util;

public class StringUtil {

    private StringUtil() {
        // Private constructor to hide the implicit public one
    }

    public static String getclassDisplayName(String fullPackageName) {
        if (fullPackageName != null) {
            // Find the last dot separator for the extension
            int lastDotIndex = fullPackageName.lastIndexOf(".");
            // Find the second last dot to locate the start of the file name
            int secondLastDotIndex = fullPackageName.lastIndexOf(".", lastDotIndex - 1);
            return fullPackageName.substring(secondLastDotIndex + 1);
        }
        return fullPackageName;
    }
    public static String cleanJavaCode(String code) {
        if (code == null || code.isBlank()) {
            return code; // Return as is for empty or null input
        }
        code = code.trim(); // Remove any leading or trailing whitespace
        // Remove leading ```java (case insensitive)
        if (code.toLowerCase().startsWith("```java")) {
            code = code.substring(7).trim(); // Remove "```java" (7 characters)
        } else if (code.toLowerCase().startsWith("```")) {
            code = code.substring(3).trim(); // Remove "```" if no "java" present
        }
        // Remove trailing ```
        if (code.endsWith("```")) {
            code = code.substring(0, code.length() - 3).trim(); // Remove last "```" (3 characters)
        }
        return code;
    }
}
