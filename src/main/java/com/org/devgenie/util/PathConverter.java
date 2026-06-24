package com.org.devgenie.util;

public class PathConverter {

    private static String clonedRepoPath = "/tmp/devgenie/repos/"; // SonarQube issue: Refactor your code to get this URI from a customizable parameter.

    // Dynamically update repo path after cloning
    public static void setClonedRepoPath(String path) {
        clonedRepoPath = path;
    }

    private PathConverter() {
        // Private constructor to prevent instantiation of this utility class
    }

    // Convert from dotted path to slashed path
    public static String toSlashedPath(String inputPath) {
        int lastDotIndex = inputPath.lastIndexOf('.');
        String convertedPath = inputPath.substring(0, lastDotIndex).replace('.', '/');
        return convertedPath + inputPath.substring(lastDotIndex);
    }

    // Convert from slashed path back to dotted path
    public static String toDottedPath(String inputPath) {
        int lastSlashIndex = inputPath.lastIndexOf('/');
        String convertedPath = inputPath.substring(0, lastSlashIndex).replace('/', '.');
        return convertedPath + inputPath.substring(lastSlashIndex);
    }
}