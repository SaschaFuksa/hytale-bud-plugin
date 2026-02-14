package com.bud.reaction.block;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.interaction.InteractionManager;
import com.bud.llm.message.block.LLMBlockManager;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

/**
 * Schedules LLM reactions after blocks are broken.
 * Similar to CombatChatScheduler but for environmental interactions.
 */
public class BlockChatScheduler {

    private static final BlockChatScheduler INSTANCE = new BlockChatScheduler();

    private final Map<UUID, Long> lastReactionTime = new ConcurrentHashMap<>();

    private final Map<UUID, ScheduledFuture<?>> pendingReactions = new ConcurrentHashMap<>();

    private static final long COOLDOWN_MS = 10_000; // 10 seconds cooldown between reactions

    private static final long DEBOUNCE_MS = 3_000; // 3 seconds debounce for consecutive blocks

    private static final InteractionManager interactionManager = InteractionManager.getInstance();

    private static final LLMBlockManager llmBlockManager = LLMBlockManager.getInstance();

    private BlockChatScheduler() {
    }

    public static BlockChatScheduler getInstance() {
        return INSTANCE;
    }

    /**
     * Called when a block event is registered.
     * Starts a short delay before triggering the LLM to allow for multiple blocks
     * being placed or broken
     * (e.g. mining a vein) and then summarizing or reacting to the latest.
     */
    public void onBlockEvent(UUID playerId) {
        long now = System.currentTimeMillis();
        long lastTime = lastReactionTime.getOrDefault(playerId, 0L);

        // Check overall cooldown
        if (now - lastTime < COOLDOWN_MS) {
            return;
        }

        // Debounce: Cancel pending task if a new block event occurs quickly
        ScheduledFuture<?> pending = pendingReactions.remove(playerId);
        if (pending != null) {
            pending.cancel(false);
        }

        // Schedule reaction with a delay to group multiple blocks (e.g. mining a vein)
        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            Thread.ofVirtual().start(() -> {
                try {
                    pendingReactions.remove(playerId);
                    lastReactionTime.put(playerId, System.currentTimeMillis());

                    LoggerUtil.getLogger().fine(() -> "[BUD] Triggering block reaction for player " + playerId);
                    interactionManager.processInteraction(
                            Collections.singleton(playerId),
                            llmBlockManager);
                } catch (Exception e) {
                    LoggerUtil.getLogger().severe(() -> "[BUD] Error in BlockChatScheduler: " + e.getMessage());
                }
            });
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);

        pendingReactions.put(playerId, future);
    }
}
