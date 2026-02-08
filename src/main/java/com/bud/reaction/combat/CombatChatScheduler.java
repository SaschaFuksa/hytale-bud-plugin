package com.bud.reaction.combat;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.interaction.InteractionManager;
import com.bud.llm.message.combat.LLMCombatManager;
import com.bud.npc.BudRegistry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

/**
 * Event-driven combat chat scheduler.
 * Instead of polling every 10 seconds, this scheduler triggers a delayed
 * LLM chat message when combat is registered.
 * 
 * Benefits:
 * - Only processes players with actual combat events
 * - No unnecessary iteration over all players
 * - Configurable delay between combat and chat response
 * - Debouncing: Multiple rapid combat events only trigger one chat
 */
public class CombatChatScheduler {

    private static final CombatChatScheduler INSTANCE = new CombatChatScheduler();

    /**
     * Delay in seconds before sending combat chat after last combat event.
     * This allows for "debouncing" - multiple hits in quick succession
     * will only result in one chat message.
     */
    private static final long COMBAT_CHAT_DELAY_SECONDS = 2L;

    /**
     * Minimum time between combat chats for the same player (cooldown).
     * Prevents spam if player is in prolonged combat.
     */
    private static final long COOLDOWN_SECONDS = 3L;

    /**
     * Tracks pending chat tasks per player.
     * If a new combat event occurs while a task is pending, we cancel and
     * reschedule.
     */
    private final Map<UUID, ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();

    /**
     * Tracks last chat time per player for cooldown enforcement.
     */
    private final Map<UUID, Long> lastChatTime = new ConcurrentHashMap<>();

    private CombatChatScheduler() {
    }

    public static CombatChatScheduler getInstance() {
        return INSTANCE;
    }

    /**
     * Called when a combat interaction is registered.
     * Schedules a delayed chat message for the player.
     * 
     * @param playerId The UUID of the player involved in combat
     */
    public void onCombatRegistered(UUID playerId) {
        // Check cooldown
        Long lastChat = lastChatTime.get(playerId);
        if (lastChat != null) {
            long elapsed = (System.currentTimeMillis() - lastChat) / 1000;
            if (elapsed < COOLDOWN_SECONDS) {
                LoggerUtil.getLogger().finer(() -> "[BUD] Combat chat on cooldown for " + playerId + " ("
                        + (COOLDOWN_SECONDS - elapsed) + "s remaining)");
                return;
            }
        }

        // Cancel any existing pending task for this player (debouncing)
        ScheduledFuture<?> existingTask = pendingTasks.get(playerId);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(false);
            LoggerUtil.getLogger()
                    .finer(() -> "[BUD] Cancelled pending combat chat for " + playerId + " (new combat event)");
        }

        // Schedule new delayed task
        ScheduledFuture<?> newTask = HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> triggerCombatChat(playerId),
                COMBAT_CHAT_DELAY_SECONDS,
                TimeUnit.SECONDS);

        pendingTasks.put(playerId, newTask);
        LoggerUtil.getLogger()
                .finer(() -> "[BUD] Scheduled combat chat for " + playerId + " in " + COMBAT_CHAT_DELAY_SECONDS + "s");
    }

    /**
     * Triggers the actual combat chat for a player.
     * Called after the delay expires.
     */
    private void triggerCombatChat(UUID playerId) {
        pendingTasks.remove(playerId);

        // Double-check player still has buds (they might have been removed during
        // delay)
        if (BudRegistry.getInstance().getByOwner(playerId).isEmpty()) {
            LoggerUtil.getLogger()
                    .finer(() -> "[BUD] Player " + playerId + " no longer has buds, skipping combat chat");
            return;
        }

        lastChatTime.put(playerId, System.currentTimeMillis());
        InteractionManager.getInstance().processInteraction(Set.of(playerId), new LLMCombatManager());
    }

    /**
     * Clears all pending tasks and cooldowns for a player.
     * Should be called on player disconnect.
     */
    public void clearPlayer(UUID playerId) {
        ScheduledFuture<?> task = pendingTasks.remove(playerId);
        if (task != null) {
            task.cancel(false);
        }
        lastChatTime.remove(playerId);
    }
}
