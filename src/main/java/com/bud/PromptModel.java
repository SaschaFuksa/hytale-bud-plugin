package com.bud;

import java.util.Map;

public class PromptModel {
    private Map<String, String> prompts;

    public Map<String, String> getPrompts() {
        return prompts;
    }

    public void setPrompts(Map<String, String> prompts) {
        this.prompts = prompts;
    }

    @Override
    public String toString() {
        return "PromptModel{prompts=" + prompts + "}";
    }
}
