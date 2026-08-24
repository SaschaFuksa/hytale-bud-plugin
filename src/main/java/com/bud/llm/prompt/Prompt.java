package com.bud.llm.prompt;

public record Prompt(String systemPrompt, String userPrompt, Integer maxTokens) {

    public Prompt(String systemPrompt, String userPrompt) {
        this(systemPrompt, userPrompt, null);
    }
}
