package com.bud.reaction.item;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.llm.message.item.LLMItemManager;
import com.bud.reaction.BaseChatScheduler;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

public class ItemChatScheduler extends BaseChatScheduler {

    private static final ItemChatScheduler INSTANCE = new ItemChatScheduler();

    private ItemChatScheduler() {
    }

    public static ItemChatScheduler getInstance() {
        return INSTANCE;
    }

    private static final LLMItemManager llmItemManager = new LLMItemManager();

    @Override
    public void onEvent(UUID playerId) {
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

        // Schedule new reaction after debounce period
        ScheduledFuture<?> future = HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            Thread.ofVirtual().start(() -> {
                try {
                    pendingReactions.remove(playerId);
                    lastReactionTime.put(playerId, System.currentTimeMillis());
                    LoggerUtil.getLogger().fine(() -> "[BUD] Triggering item reaction for player " + playerId);
                    interactionManager.processInteraction(
                            Collections.singleton(playerId),
                            llmItemManager);
                } catch (Exception e) {
                    LoggerUtil.getLogger().severe(() -> "[BUD] Error in ItemChatScheduler: " + e.getMessage());
                }
            });
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);

        pendingReactions.put(playerId, future);
    }

}
