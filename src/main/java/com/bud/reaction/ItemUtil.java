package com.bud.reaction;

public class ItemUtil {

    public static String getItemName(String id) {
        if (id.contains(":")) {
            id = id.split(":")[1];
        }
        id = id.replaceAll("_", " ");
        return id;
    }

}
