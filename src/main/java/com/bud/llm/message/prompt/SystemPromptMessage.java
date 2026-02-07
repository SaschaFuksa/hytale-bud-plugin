package com.bud.llm.message.prompt;

import java.util.Map;
import java.nio.file.Path;

public class SystemPromptMessage extends AbstractYamlMessage {

    private Map<String, String> prompts;

    public Map<String, String> getPrompts() {
        return prompts;
    }

    public static SystemPromptMessage load(Path path) {
        return loadFromFile(SystemPromptMessage.class, path);
    }
}
