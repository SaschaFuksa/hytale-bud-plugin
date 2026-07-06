package com.bud.llm.client;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonUtils {

    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("(?s)\\{.*\\}");
    private static final Pattern JSON_STRING_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\\\"])*)\"");
    private static final Pattern JSON_NUMBER_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(-?\\d+)");
    private static final Pattern JSON_BOOLEAN_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(true|false)");
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern JSON_ARRAY_STRING_PATTERN = Pattern.compile("\"((?:\\\\.|[^\\\"])*)\"");

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

    /**
     * Extracts the first {@code {...}} JSON object from a raw LLM response, tolerating
     * surrounding prose. Falls back to the trimmed raw response if no object is found.
     */
    public static String extractJsonObject(String rawResponse) {
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(rawResponse);
        return matcher.find() ? matcher.group() : rawResponse.trim();
    }

    public static String extractString(String json, String key) {
        Pattern pattern = Pattern.compile(JSON_STRING_PATTERN.pattern().formatted(Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        String groupValue = matcher.group(1);
        return groupValue == null ? null : decodeString(groupValue);
    }

    public static Integer extractInt(String json, String key) {
        Pattern pattern = Pattern.compile(JSON_NUMBER_PATTERN.pattern().formatted(Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return Integer.valueOf(matcher.group(1));
    }

    public static boolean extractBoolean(String json, String key) {
        Pattern pattern = Pattern.compile(JSON_BOOLEAN_PATTERN.pattern().formatted(Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    public static Set<String> extractStringArray(String json, String key) {
        Pattern pattern = Pattern.compile(JSON_ARRAY_PATTERN.pattern().formatted(Pattern.quote(key)), Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return Set.of();
        }
        String arrayBody = matcher.group(1);
        if (arrayBody == null) {
            return Set.of();
        }
        Matcher valueMatcher = JSON_ARRAY_STRING_PATTERN.matcher(arrayBody);
        Set<String> values = new HashSet<>();
        while (valueMatcher.find()) {
            String groupValue = valueMatcher.group(1);
            if (groupValue != null) {
                values.add(decodeString(groupValue));
            }
        }
        return values;
    }

    private static String decodeString(String value) {
        return value.replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim();
    }
}
