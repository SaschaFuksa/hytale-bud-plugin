package com.bud.llm.client;

public final class AuthorizationHeaderUtils {

    private AuthorizationHeaderUtils() {
    }

    public static String buildAuthorizationHeader(String apiKey) {
        if (apiKey == null || apiKey.isBlank() || "not_needed".equalsIgnoreCase(apiKey)) {
            return null;
        }

        return apiKey.regionMatches(true, 0, "Bearer ", 0, 7) ? apiKey : "Bearer " + apiKey;
    }
}
