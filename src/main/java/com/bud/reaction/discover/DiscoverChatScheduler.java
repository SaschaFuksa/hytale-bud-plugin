package com.bud.reaction.discover;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.llm.message.discover.LLMDiscoverManager;
import com.bud.reaction.BaseChatScheduler;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

/**
 * Schedules LLM reactions after zone discoveries.
 * Uses debouncing to prevent rapid fire on multiple quick discoveries.
 */
public class DiscoverChatScheduler extends BaseChatScheduler {

    private static final DiscoverChatScheduler INSTANCE = new DiscoverChatScheduler();
    private static final LLMDiscoverManager llmDiscoverManager = LLMDiscoverManager.getInstance();

    private DiscoverChatScheduler() {
    }

    public static DiscoverChatScheduler getInstance() {
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
                            .fine(() -> "[BUD] Triggering discover reaction for player " + playerId);
                    interactionManager.processInteraction(
                            Collections.singleton(playerId),
                            llmDiscoverManager);
                } catch (Exception e) {
                    LoggerUtil.getLogger()
                            .severe(() -> "[BUD] Error in DiscoverChatScheduler: " + e.getMessage());
                }
            });
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);

        pendingReactions.put(playerId, future);
    }
}
