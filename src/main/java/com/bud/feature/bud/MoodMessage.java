package com.bud.feature.bud;

import java.nio.file.Path;
import java.util.Map;

import com.bud.llm.messages.AbstractYamlMessage;

public class MoodMessage extends AbstractYamlMessage {

    private Map<String, String> mood;

    public Map<String, String> getMood() {
        return mood;
    }

    public static MoodMessage load(Path path) {
        return loadFromFile(MoodMessage.class, path);
    }

}
