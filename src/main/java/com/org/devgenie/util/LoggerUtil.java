package com.org.devgenie.util;

public class LoggerUtil {
    /**
     * Masks sensitive data in a string. Extend this logic as needed.
     */
    public static String maskSensitive(String input) {
        if (input == null) return null;
        // Simple example: mask everything except first and last char
        if (input.length() <= 2) return "**";
        return input.charAt(0) + "***" + input.charAt(input.length() - 1);
    }

    /**
     * Masks sensitive data in an array of objects for logger arguments.
     */
    public static Object[] maskSensitive(Object... args) {
        if (args == null) return null;
        Object[] masked = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof String) {
                masked[i] = maskSensitive((String) args[i]);
            } else {
                masked[i] = args[i];
            }
        }
        return masked;
    }
}
