package com.bud.feature;

import java.nio.file.Path;
import java.util.Map;

import com.bud.llm.messages.AbstractYamlMessage;

public class SystemPromptMessage extends AbstractYamlMessage {

    private Map<String, String> prompts;

    public Map<String, String> getPrompts() {
        return prompts;
    }

    public static SystemPromptMessage load(Path path) {
        return loadFromFile(SystemPromptMessage.class, path);
    }
}
