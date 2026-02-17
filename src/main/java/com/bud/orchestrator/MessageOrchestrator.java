package com.bud.orchestrator;

import java.util.Collections;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.BudConfig;
import com.bud.interaction.InteractionManager;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

/**
 * Central message orchestrator that replaces individual per-domain schedulers.
 * <p>
 * Events from all systems (combat, discover, crafting, items, weather, etc.)
 * are enqueued here. The orchestrator enforces:
 * <ul>
 * <li>A global cooldown between ANY bud message for a player</li>
 * <li>A per-channel cooldown (AMBIENT, ACTIVITY, COMBAT)</li>
 * <li>Max queue depth per channel — lowest priority events get dropped</li>
 * <li>Priority-based dispatch within each channel</li>
 * <li>Channel alternation to prevent one channel from starving another</li>
 * </ul>
 * <p>
 * The orchestrator runs a periodic tick that checks all player queues
 * and dispatches the next eligible event.
 */
public class MessageOrchestrator {

    private static final MessageOrchestrator INSTANCE = new MessageOrchestrator();

    // --- Per-player state ---

    /**
     * Per player → per channel → priority queue of pending events.
     */
    private final Map<UUID, Map<MessageChannel, PriorityQueue<QueuedEvent>>> queues = new ConcurrentHashMap<>();

    /**
     * Per player: timestamp of last message sent (any channel).
     */
    private final Map<UUID, Long> lastGlobalMessage = new ConcurrentHashMap<>();

    /**
     * Per player → per channel: timestamp of last message sent on that channel.
     */
    private final Map<UUID, Map<MessageChannel, Long>> lastChannelMessage = new ConcurrentHashMap<>();

    /**
     * Per player: which channel was served last (for alternation).
     */
    private final Map<UUID, MessageChannel> lastServedChannel = new ConcurrentHashMap<>();

    // --- Tick handle ---
    private ScheduledFuture<?> tickTask;

    private final InteractionManager interactionManager = InteractionManager.getInstance();

    private MessageOrchestrator() {
    }

    public static MessageOrchestrator getInstance() {
        return INSTANCE;
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Enqueue a new event for a player. If the channel queue is full,
     * the lowest-priority (highest number) event is dropped to make room.
     */
    public void enqueue(QueuedEvent event) {
        PriorityQueue<QueuedEvent> queue = getOrCreateQueue(event.playerId(), event.channel());
        int maxDepth = BudConfig.getInstance().getOrchestratorMaxQueueDepth();

        synchronized (queue) {
            // Deduplication: replace existing event of the same type instead of stacking
            boolean replaced = queue.removeIf(e -> e.eventType().equals(event.eventType()));
            if (replaced) {
                LoggerUtil.getLogger().finer(() -> "[Orchestrator] Deduplicated " + event.eventType()
                        + " for player " + event.playerId() + " in channel " + event.channel());
            }

            // Drop lowest priority if full
            if (queue.size() >= maxDepth) {
                QueuedEvent lowest = findLowestPriority(queue);
                if (lowest != null && event.priority() < lowest.priority()) {
                    queue.remove(lowest);
                    LoggerUtil.getLogger().finer(() -> "[Orchestrator] Dropped event " + lowest.eventType()
                            + " (prio " + lowest.priority() + ") for player " + event.playerId()
                            + " in channel " + event.channel());
                } else if (lowest != null && event.priority() >= lowest.priority()) {
                    // New event is not higher priority than anything in queue — drop it
                    LoggerUtil.getLogger().finer(() -> "[Orchestrator] Queue full, dropping incoming event "
                            + event.eventType() + " (prio " + event.priority() + ") for player "
                            + event.playerId());
                    return;
                }
            }

            queue.add(event);
        }

        LoggerUtil.getLogger().fine(() -> "[Orchestrator] Enqueued " + event.eventType()
                + " (prio " + event.priority() + ", channel " + event.channel()
                + ") for player " + event.playerId());
    }

    /**
     * Start the orchestrator tick loop.
     * Should be called once on server start / first player connect.
     */
    public synchronized void start() {
        if (tickTask != null) {
            return;
        }
        long tickMs = BudConfig.getInstance().getOrchestratorTickIntervalMs();
        tickTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::tick, tickMs, tickMs, TimeUnit.MILLISECONDS);
        LoggerUtil.getLogger().info(() -> "[Orchestrator] Started with tick interval " + tickMs + "ms");
    }

    /**
     * Stop the orchestrator tick loop.
     */
    public synchronized void stop() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
            LoggerUtil.getLogger().info(() -> "[Orchestrator] Stopped.");
        }
    }

    /**
     * Clear all state for a disconnecting player.
     */
    public void clearPlayer(UUID playerId) {
        queues.remove(playerId);
        lastGlobalMessage.remove(playerId);
        lastChannelMessage.remove(playerId);
        lastServedChannel.remove(playerId);
        LoggerUtil.getLogger().fine(() -> "[Orchestrator] Cleared state for player " + playerId);
    }

    // =========================================================================
    // Tick logic
    // =========================================================================

    /**
     * Called periodically. Iterates all players with queued events
     * and dispatches at most one event per player per tick.
     */
    private void tick() {
        try {
            long now = System.currentTimeMillis();
            long globalCooldown = BudConfig.getInstance().getOrchestratorGlobalCooldownMs();
            long channelCooldown = BudConfig.getInstance().getOrchestratorChannelCooldownMs();

            for (UUID playerId : queues.keySet()) {
                // Global cooldown check
                long lastGlobal = lastGlobalMessage.getOrDefault(playerId, 0L);
                if (now - lastGlobal < globalCooldown) {
                    continue;
                }

                // Pick channel: prefer the one that waited longest, alternating
                MessageChannel channel = pickChannel(playerId, now, channelCooldown);
                if (channel == null) {
                    continue; // all channels on cooldown or empty
                }

                PriorityQueue<QueuedEvent> queue = getQueue(playerId, channel);
                if (queue == null || queue.isEmpty()) {
                    continue;
                }

                QueuedEvent event;
                synchronized (queue) {
                    event = queue.poll();
                }
                if (event == null) {
                    continue;
                }

                // Dispatch
                dispatch(event);

                // Update timestamps
                lastGlobalMessage.put(playerId, now);
                lastChannelMessage.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                        .put(channel, now);
                lastServedChannel.put(playerId, channel);
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[Orchestrator] Error in tick: " + e.getMessage());
        }
    }

    /**
     * Pick the best channel to serve for a player.
     * Prefers channels that have pending events and are not on cooldown.
     * Among those, prefers the channel that was NOT served last (alternation)
     * and among equals, picks the one with the highest-priority head event.
     */
    private MessageChannel pickChannel(UUID playerId, long now, long channelCooldown) {
        Map<MessageChannel, PriorityQueue<QueuedEvent>> playerQueues = queues.get(playerId);
        if (playerQueues == null) {
            return null;
        }

        Map<MessageChannel, Long> channelTimestamps = lastChannelMessage.getOrDefault(
                playerId, Map.of());
        MessageChannel lastServed = lastServedChannel.get(playerId);

        MessageChannel bestChannel = null;
        int bestPriority = Integer.MAX_VALUE;
        boolean bestIsAlternate = false; // true if this channel != lastServed

        for (MessageChannel channel : MessageChannel.values()) {
            PriorityQueue<QueuedEvent> queue = playerQueues.get(channel);
            if (queue == null || queue.isEmpty()) {
                continue;
            }

            // Channel cooldown check
            long lastTime = channelTimestamps.getOrDefault(channel, 0L);
            if (now - lastTime < channelCooldown) {
                continue;
            }

            QueuedEvent head;
            synchronized (queue) {
                head = queue.peek();
            }
            if (head == null) {
                continue;
            }

            boolean isAlternate = !channel.equals(lastServed);

            // Prefer: alternate channel > higher priority head event
            if (bestChannel == null
                    || (isAlternate && !bestIsAlternate)
                    || (isAlternate == bestIsAlternate && head.priority() < bestPriority)) {
                bestChannel = channel;
                bestPriority = head.priority();
                bestIsAlternate = isAlternate;
            }
        }

        return bestChannel;
    }

    /**
     * Dispatch an event: calls InteractionManager on a virtual thread.
     */
    private void dispatch(QueuedEvent event) {
        LoggerUtil.getLogger().fine(() -> "[Orchestrator] Dispatching " + event.eventType()
                + " (prio " + event.priority() + ", channel " + event.channel()
                + ") for player " + event.playerId());

        Thread.ofVirtual().start(() -> {
            try {
                interactionManager.processInteraction(
                        Collections.singleton(event.playerId()),
                        event.chatManager());
            } catch (Exception e) {
                LoggerUtil.getLogger().severe(() -> "[Orchestrator] Error dispatching " + event.eventType()
                        + " for player " + event.playerId() + ": " + e.getMessage());
            }
        });
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PriorityQueue<QueuedEvent> getOrCreateQueue(UUID playerId, MessageChannel channel) {
        return queues.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(channel, k -> new PriorityQueue<>());
    }

    private PriorityQueue<QueuedEvent> getQueue(UUID playerId, MessageChannel channel) {
        Map<MessageChannel, PriorityQueue<QueuedEvent>> playerQueues = queues.get(playerId);
        if (playerQueues == null) {
            return null;
        }
        return playerQueues.get(channel);
    }

    /**
     * Find the event with the lowest priority (highest number) in the queue.
     */
    private QueuedEvent findLowestPriority(PriorityQueue<QueuedEvent> queue) {
        QueuedEvent lowest = null;
        for (QueuedEvent e : queue) {
            if (lowest == null || e.priority() > lowest.priority()) {
                lowest = e;
            }
        }
        return lowest;
    }
}
