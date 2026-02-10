package com.bud.llm.message.prompt;

import java.nio.file.Path;
import java.util.Map;

public class MoodPromptMessage extends AbstractYamlMessage {

    private Map<String, String> mood;

    public Map<String, String> getMood() {
        return mood;
    }

    public static MoodPromptMessage load(Path path) {
        return loadFromFile(MoodPromptMessage.class, path);
    }

}
