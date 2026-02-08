package com.bud.reaction.block;

import java.util.UUID;

import com.bud.npc.BudRegistry;

public class BlockUtil {

    public static String getBlockName(String id) {
        if (id.contains(":")) {
            id = id.split(":")[1];
        }
        id = id.replaceAll("_", " ");
        return id;
    }

    public static boolean playerHasBud(UUID playerId) {
        return !BudRegistry.getInstance().getByOwner(playerId).isEmpty();
    }
}
