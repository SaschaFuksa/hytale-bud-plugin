package com.bud.util;

/**
 * Utility class for JSON string operations.
 * Centralizes JSON escaping logic to avoid code duplication.
 */
public final class JsonUtils {

    private JsonUtils() {
        // Utility class - no instantiation
    }

    /**
     * Escapes a string for safe inclusion in JSON.
     * Handles null input by returning empty string.
     *
     * @param input The string to escape
     * @return The escaped string (without surrounding quotes)
     */
    public static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Escapes a string and wraps it in JSON quotes.
     * Handles null input by returning empty quoted string.
     *
     * @param input The string to escape and quote
     * @return The escaped string with surrounding quotes
     */
    public static String escapeJsonWithQuotes(String input) {
        return "\"" + escapeJson(input) + "\"";
    }
}
