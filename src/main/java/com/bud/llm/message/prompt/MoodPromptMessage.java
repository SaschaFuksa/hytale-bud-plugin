package com.bud.llm.message.prompt;

import java.nio.file.Path;
import java.util.Map;

public class MoodPromptMessage extends AbstractYamlMessage {

    private Map<String, String> prompts;

    public Map<String, String> getPrompts() {
        return prompts;
    }

    public static MoodPromptMessage load(Path path) {
        return loadFromFile(MoodPromptMessage.class, path);
    }

}
