package com.bud.reaction.crafting;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.llm.message.craft.LLMCraftManager;
import com.bud.reaction.BaseChatScheduler;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

/**
 * Schedules LLM reactions after crafting events.
 * Uses debouncing to prevent rapid fire on multiple quick crafts.
 */
public class CraftChatScheduler extends BaseChatScheduler {

    private static final CraftChatScheduler INSTANCE = new CraftChatScheduler();
    private static final LLMCraftManager llmCraftManager = LLMCraftManager.getInstance();

    private CraftChatScheduler() {
    }

    public static CraftChatScheduler getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEvent(UUID playerId) {
        long now = System.currentTimeMillis();
        long lastTime = lastReactionTime.getOrDefault(playerId, 0L);
        if (now - lastTime < COOLDOWN_MS) {
            return;
        }

        ScheduledFuture<?> pending = pendingReactions.remove(playerId);
        if (pending != null) {
            pending.cancel(false);
        }

        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            Thread.ofVirtual().start(() -> {
                try {
                    pendingReactions.remove(playerId);
                    lastReactionTime.put(playerId, System.currentTimeMillis());
                    LoggerUtil.getLogger()
                            .fine(() -> "[BUD] Triggering craft reaction for player " + playerId);
                    interactionManager.processInteraction(
                            Collections.singleton(playerId),
                            llmCraftManager);
                } catch (Exception e) {
                    LoggerUtil.getLogger()
                            .severe(() -> "[BUD] Error in CraftChatScheduler: " + e.getMessage());
                }
            });
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);

        pendingReactions.put(playerId, future);
    }
}
