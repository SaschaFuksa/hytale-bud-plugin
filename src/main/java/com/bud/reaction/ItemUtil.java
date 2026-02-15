package com.bud.reaction;

import java.util.Map;

public class ItemUtil {

    private static final Map<String, String> ITEM_NAME_MAP = Map.of(
            "healt1", "Blood Rose",
            "healt2", "Bloodcap Mushroom",
            "healt3", "Blood Leaf",
            "mana1", "Azure Fern",
            "mana2", "Azurecap Mushroom",
            "mana3", "Azure Kelp",
            "stamina1", "Strom Thistle",
            "stamina2", "Stormcap Mushroom",
            "stamina3", "Storm Sapling");

    public static String getDisplayName(String id) {
        if (id.contains(":")) {
            id = id.split(":")[1];
        }
        id = id.replaceAll("_", " ");
        return ITEM_NAME_MAP.getOrDefault(id, id);
    }

}
