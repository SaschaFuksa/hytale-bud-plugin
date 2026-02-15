package com.bud.llm.message.prompt;

import java.nio.file.Path;
import java.util.Map;

public class SystemPromptMessage extends BaseYamlMessage {

    private Map<String, String> prompts;

    public Map<String, String> getPrompts() {
        return prompts;
    }

    public static SystemPromptMessage load(Path path) {
        return loadFromFile(SystemPromptMessage.class, path);
    }
}
