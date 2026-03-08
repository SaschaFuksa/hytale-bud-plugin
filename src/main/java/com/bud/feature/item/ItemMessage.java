package com.bud.feature.item;

import java.nio.file.Path;
import java.util.Map;

import com.bud.llm.messages.AbstractYamlMessage;

public class ItemMessage extends AbstractYamlMessage {

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

    public static ItemMessage load(Path path) {
        return loadFromFile(ItemMessage.class, path);
    }

}
