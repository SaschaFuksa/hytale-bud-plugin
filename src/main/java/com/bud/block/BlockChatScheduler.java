package com.bud.block;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bud.interaction.InteractionManager;
import com.bud.llm.message.block.LLMBlockManager;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

import java.util.Collections;

/**
 * Schedules LLM reactions after blocks are broken.
 * Similar to CombatChatScheduler but for environmental interactions.
 */
public class BlockChatScheduler {

    private static final BlockChatScheduler INSTANCE = new BlockChatScheduler();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private BlockChatScheduler() {
    }

    public static BlockChatScheduler getInstance() {
        return INSTANCE;
    }

    /**
     * Called when a block event is registered.
     * Starts a short delay before triggering the LLM to allow for multiple blocks
     * being broken
     * (e.g. mining a vein) and then summarizing or reacting to the latest.
     */
    public void onBlockBroken(UUID playerId) {
        // Schedule reaction with a delay (e.g. 2 seconds) to not spam for every single
        // block
        scheduler.schedule(() -> {
            try {
                LoggerUtil.getLogger().fine(() -> "[BUD] Triggering block reaction for player " + playerId);
                InteractionManager.getInstance().processInteraction(
                        Collections.singleton(playerId),
                        LLMBlockManager.getInstance());
            } catch (Exception e) {
                LoggerUtil.getLogger().severe(() -> "[BUD] Error in BlockChatScheduler: " + e.getMessage());
            }
        }, 2, TimeUnit.SECONDS);
    }
}
