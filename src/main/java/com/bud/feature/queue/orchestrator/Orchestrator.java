package com.bud.feature.queue.orchestrator;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.bud.core.config.OrchestratorConfig;
import com.bud.feature.LLMInteractionManager;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.HytaleServer;

public class Orchestrator {

    private static final Orchestrator INSTANCE = new Orchestrator();

    private final Map<UUID, Map<OrchestratorChannel, PriorityQueue<OrchestratorQueue>>> queues = new ConcurrentHashMap<>();

    private final Map<UUID, Long> lastGlobalMessage = new ConcurrentHashMap<>();

    private final Map<UUID, Map<OrchestratorChannel, Long>> lastChannelMessage = new ConcurrentHashMap<>();

    private final Map<UUID, OrchestratorChannel> lastServedChannel = new ConcurrentHashMap<>();

    private ScheduledFuture<?> tickTask;

    private final LLMInteractionManager interactionManager = LLMInteractionManager.getInstance();

    private Orchestrator() {
    }

    public static Orchestrator getInstance() {
        return INSTANCE;
    }

    public void enqueue(OrchestratorQueue event) {
        UUID playerId = event.interactionEntry().getPlayerId();
        PriorityQueue<OrchestratorQueue> queue = getOrCreateQueue(playerId, event.channel());
        int maxDepth = OrchestratorConfig.getInstance().getOrchestratorMaxQueueDepth();

        synchronized (queue) {
            boolean replaced = queue.removeIf(e -> e.eventType().equals(event.eventType()));
            if (replaced) {
                LoggerUtil.getLogger().finer(() -> "[Orchestrator] Deduplicated " + event.eventType()
                        + " for player " + playerId + " in channel " + event.channel());
            }

            if (queue.size() >= maxDepth) {
                OrchestratorQueue lowest = findLowestPriority(queue);
                if (lowest != null && event.entry().getPriority() < lowest.entry().getPriority()) {
                    queue.remove(lowest);
                    LoggerUtil.getLogger().finer(() -> "[Orchestrator] Dropped event " + lowest.eventType()
                            + " for player " + playerId
                            + " in channel " + event.channel());
                } else if (lowest != null && event.entry().getPriority() >= lowest.entry().getPriority()) {
                    LoggerUtil.getLogger().finer(() -> "[Orchestrator] Queue full, dropping incoming event "
                            + event.eventType() + " for player "
                            + playerId);
                    return;
                }
            }

            queue.add(event);
        }

        LoggerUtil.getLogger().fine(() -> "[Orchestrator] Enqueued " + event.eventType()
                + ", channel " + event.channel()
                + ") for player " + playerId);
    }

    public synchronized void start() {
        if (tickTask != null) {
            return;
        }
        long tickMs = OrchestratorConfig.getInstance().getOrchestratorTickIntervalMs();
        tickTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::tick, tickMs, tickMs, TimeUnit.MILLISECONDS);
        LoggerUtil.getLogger().info(() -> "[Orchestrator] Started with tick interval " + tickMs + "ms");
    }

    public synchronized void stop() {
        if (tickTask != null) {
            tickTask.cancel(false);
            tickTask = null;
            LoggerUtil.getLogger().info(() -> "[Orchestrator] Stopped.");
        }
    }

    public void clearPlayer(UUID playerId) {
        queues.remove(playerId);
        lastGlobalMessage.remove(playerId);
        lastChannelMessage.remove(playerId);
        lastServedChannel.remove(playerId);
        LoggerUtil.getLogger().fine(() -> "[Orchestrator] Cleared state for player " + playerId);
    }

    private void tick() {
        try {
            long now = System.currentTimeMillis();
            long globalCooldown = OrchestratorConfig.getInstance().getOrchestratorGlobalCooldownMs();
            long channelCooldown = OrchestratorConfig.getInstance().getOrchestratorChannelCooldownMs();

            for (UUID playerId : queues.keySet()) {
                long lastGlobal = lastGlobalMessage.getOrDefault(playerId, 0L);
                if (now - lastGlobal < globalCooldown) {
                    continue;
                }

                OrchestratorChannel channel = pickChannel(playerId, now, channelCooldown);
                if (channel == null) {
                    continue; // all channels on cooldown or empty
                }

                PriorityQueue<OrchestratorQueue> queue = getQueue(playerId, channel);
                if (queue == null || queue.isEmpty()) {
                    continue;
                }

                OrchestratorQueue event;
                synchronized (queue) {
                    event = queue.poll();
                }
                if (event == null) {
                    continue;
                }

                dispatch(event);

                lastGlobalMessage.put(playerId, now);
                lastChannelMessage.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                        .put(channel, now);
                lastServedChannel.put(playerId, channel);
            }
        } catch (Exception e) {
            LoggerUtil.getLogger().severe(() -> "[Orchestrator] Error in tick: " + e.getMessage());
        }
    }

    private OrchestratorChannel pickChannel(UUID playerId, long now, long channelCooldown) {
        Map<OrchestratorChannel, PriorityQueue<OrchestratorQueue>> playerQueues = queues.get(playerId);
        if (playerQueues == null) {
            return null;
        }

        Map<OrchestratorChannel, Long> channelTimestamps = lastChannelMessage.getOrDefault(
                playerId, Map.of());
        OrchestratorChannel lastServed = lastServedChannel.get(playerId);

        OrchestratorChannel bestChannel = null;
        int bestPriority = Integer.MAX_VALUE;
        boolean bestIsAlternate = false; // true if this channel != lastServed

        for (OrchestratorChannel channel : OrchestratorChannel.values()) {
            PriorityQueue<OrchestratorQueue> queue = playerQueues.get(channel);
            if (queue == null || queue.isEmpty()) {
                continue;
            }

            long lastTime = channelTimestamps.getOrDefault(channel, 0L);
            if (now - lastTime < channelCooldown) {
                continue;
            }

            OrchestratorQueue head;
            synchronized (queue) {
                head = queue.peek();
            }
            if (head == null) {
                continue;
            }

            boolean isAlternate = !channel.equals(lastServed);

            if (bestChannel == null
                    || (isAlternate && !bestIsAlternate)
                    || (isAlternate == bestIsAlternate && head.entry().getPriority() < bestPriority)) {
                bestChannel = channel;
                bestPriority = head.entry().getPriority();
                bestIsAlternate = isAlternate;
            }
        }

        return bestChannel;
    }

    private void dispatch(OrchestratorQueue event) {
        Thread.ofVirtual().start(() -> {
            try {
                LLMInteractionEntry entry = event.interactionEntry();
                if (entry == null) {
                    LoggerUtil.getLogger().severe(() -> "[Orchestrator] Missing interaction entry for event: " + event);
                    return;
                }
                interactionManager.processInteraction(entry);
            } catch (Exception e) {
                LoggerUtil.getLogger().severe(() -> "[Orchestrator] Error dispatching " + event.eventType()
                        + " for player "
                        + event.interactionEntry().budComponent().getPlayerRef().getUsername() + ": "
                        + e.getMessage());
            }
        });
    }

    private PriorityQueue<OrchestratorQueue> getOrCreateQueue(UUID playerId, OrchestratorChannel channel) {
        return queues.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(channel, k -> new PriorityQueue<>());
    }

    private PriorityQueue<OrchestratorQueue> getQueue(UUID playerId, OrchestratorChannel channel) {
        Map<OrchestratorChannel, PriorityQueue<OrchestratorQueue>> playerQueues = queues.get(playerId);
        if (playerQueues == null) {
            return null;
        }
        return playerQueues.get(channel);
    }

    private OrchestratorQueue findLowestPriority(PriorityQueue<OrchestratorQueue> queue) {
        OrchestratorQueue lowest = null;
        for (OrchestratorQueue e : queue) {
            if (lowest == null || e.entry().getPriority() > lowest.entry().getPriority()) {
                lowest = e;
            }
        }
        return lowest;
    }
}
