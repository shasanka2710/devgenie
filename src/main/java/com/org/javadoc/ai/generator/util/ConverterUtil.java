package com.org.javadoc.ai.generator.util;

public class ConverterUtil {

    private ConverterUtil() {
        // Private constructor to hide the implicit public one
    }

    public static double convertToHours(String numberString) {
        try {
            int number = Integer.parseInt(numberString);
            double hours = number / 60.0;
            return Math.round(hours * 100.0) / 100.0;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid input: " + numberString);
        }
    }

    public static double convertToHours(int number) {
        return Math.round((number / 60.0) * 100.0) / 100.0;
    }

    public static double roundToTwoDecimalPlaces(double value) {
        return Double.parseDouble(String.format("%.2f", value));
    }
}