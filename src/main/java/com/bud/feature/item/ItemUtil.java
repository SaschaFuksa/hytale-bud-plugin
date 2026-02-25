package com.bud.feature.item;

import java.util.Map;

public class ItemUtil {

    private static final Map<String, String> ITEM_NAME_MAP = Map.of(
            "Health1", "Blood Rose",
            "Health2", "Bloodcap Mushroom",
            "Health3", "Blood Leaf",
            "Mana1", "Azure Fern",
            "Mana2", "Azurecap Mushroom",
            "Mana3", "Azure Kelp",
            "Stamina1", "Storm Thistle",
            "Stamina2", "Stormcap Mushroom",
            "Stamina3", "Storm Sapling");

    public static String getDisplayName(String id) {
        if (id.contains(":")) {
            id = id.split(":")[1];
        }
        id = id.replaceAll("_", " ");

        for (Map.Entry<String, String> entry : ITEM_NAME_MAP.entrySet()) {
            if (id.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        return id;
    }

}
