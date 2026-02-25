package com.bud.llm.client;

public final class JsonUtils {

    private JsonUtils() {
    }

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

    public static String escapeJsonWithQuotes(String input) {
        return "\"" + escapeJson(input) + "\"";
    }
}
