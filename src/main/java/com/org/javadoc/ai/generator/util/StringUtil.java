package com.org.javadoc.ai.generator.util;

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
}
