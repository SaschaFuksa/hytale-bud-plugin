package com.bud.feature.work;

import javax.annotation.Nullable;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

public final class WorkstationWoodUtil {

    private static final String WOOD_BLOCK_PREFIX = "Wood_";

    private WorkstationWoodUtil() {
    }

    public static boolean isWoodBlock(@Nullable BlockType blockType) {
        if (blockType == null) {
            return false;
        }
        String blockId = blockType.getId();
        return blockId != null && blockId.startsWith(WOOD_BLOCK_PREFIX);
    }

}
