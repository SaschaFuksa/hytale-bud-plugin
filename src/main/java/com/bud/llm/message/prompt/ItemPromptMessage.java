package com.bud.llm.message.prompt;

import java.nio.file.Path;
import java.util.Map;

public class ItemPromptMessage extends BaseYamlMessage {

    private Map<String, String> pickup;
    private Map<String, String> inventory;
    private Map<String, String> bench;

    public Map<String, String> getPickup() {
        return pickup;
    }

    public Map<String, String> getInventory() {
        return inventory;
    }

    public Map<String, String> getBench() {
        return bench;
    }

    public static ItemPromptMessage load(Path path) {
        return loadFromFile(ItemPromptMessage.class, path);
    }

}
