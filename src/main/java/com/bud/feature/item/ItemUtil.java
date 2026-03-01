package com.bud.feature.item;

import java.util.Map;

import javax.annotation.Nonnull;

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

    @Nonnull
    public static String getDisplayName(@Nonnull String id) {
        String displayName = "";
        if (id.contains(":")) {
            displayName = id.split(":")[1];
        }
        displayName = displayName.replaceAll("_", " ");

        for (Map.Entry<String, String> entry : ITEM_NAME_MAP.entrySet()) {
            if (displayName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                String value = entry.getValue();
                if (value != null) {
                    return value;
                }
            }
        }

        return displayName != null ? displayName : "";
    }

}
