package com.bud.llm;

public final class LLMErrorMessageUtils {

    private LLMErrorMessageUtils() {
    }

    public static String buildUserFacingMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }

        String normalized = errorMessage.toLowerCase();
        if (normalized.contains("401") || normalized.contains("invalid_api_key")
                || normalized.contains("invalid request") || normalized.contains("authorization header")
                || normalized.contains("bearer") || normalized.contains("api token")
                || normalized.contains("api key")) {
            return "I couldn't reach the language model because the configured API token is missing or invalid.";
        }

        return null;
    }
}
