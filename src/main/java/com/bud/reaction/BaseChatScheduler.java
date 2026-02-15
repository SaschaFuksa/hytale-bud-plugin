package com.bud.reaction;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import com.bud.interaction.InteractionManager;

public abstract class BaseChatScheduler {

    protected final Map<UUID, Long> lastReactionTime = new ConcurrentHashMap<>();

    protected final Map<UUID, ScheduledFuture<?>> pendingReactions = new ConcurrentHashMap<>();

    protected static final long COOLDOWN_MS = 5_000; // 5 seconds cooldown between reactions

    protected static final long DEBOUNCE_MS = 3_000; // 3 seconds debounce for consecutive blocks

    protected static final InteractionManager interactionManager = InteractionManager.getInstance();

    public abstract void onEvent(UUID playerId);

    /**
     * Clears all pending tasks and cooldowns for a player.
     * Should be called on player disconnect.
     */
    public void clearPlayer(UUID playerId) {
        ScheduledFuture<?> task = pendingReactions.remove(playerId);
        if (task != null) {
            task.cancel(false);
        }
        lastReactionTime.remove(playerId);
    }
}
