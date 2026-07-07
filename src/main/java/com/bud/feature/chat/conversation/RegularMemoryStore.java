package com.bud.feature.chat.conversation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

final class RegularMemoryStore {

    private final Map<String, List<ConversationMemoryEntry>> entriesByBud = new ConcurrentHashMap<>();

    @Nonnull
    private static String regularKey(@Nonnull String normalizedOwnerKey, @Nonnull String budName) {
        return normalizedOwnerKey + "::" + normalize(budName);
    }

    @Nonnull
    List<ConversationMemoryEntry> getForOwner(@Nonnull String normalizedOwnerKey) {
        return Objects.requireNonNull(List.copyOf(collectForOwner(normalizedOwnerKey)));
    }

    void restoreForOwner(@Nonnull String normalizedOwnerKey, @Nonnull List<ConversationMemoryEntry> memories) {
        for (ConversationMemoryEntry entry : memories) {
            String key = regularKey(normalizedOwnerKey, entry.speakerName());
            this.entriesByBud.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
        }
    }

    void addDecayedAndNew(@Nonnull String normalizedOwnerKey, @Nonnull String budName,
            @Nonnull ConversationMemoryEntry newEntry, double decayFactor, int maxDepth) {
        String key = regularKey(normalizedOwnerKey, budName);
        List<ConversationMemoryEntry> existing = new ArrayList<>(this.entriesByBud.getOrDefault(key, List.of()));
        List<ConversationMemoryEntry> decayed = new ArrayList<>(existing.size() + 1);
        for (ConversationMemoryEntry entry : existing) {
            decayed.add(entry.decay(decayFactor));
        }
        decayed.add(newEntry);
        this.entriesByBud.put(key, sortAndCap(normalizedOwnerKey, decayed, maxDepth));
        LoggerUtil.getLogger().info(() -> "[BUD] Added memory for player " + normalizedOwnerKey
                + " from " + newEntry.speakerName()
                + " with importance " + newEntry.importance()
                + ": " + newEntry.summary());
    }

    void addManual(@Nonnull String normalizedOwnerKey, @Nonnull String budName,
            @Nonnull ConversationMemoryEntry entry, int maxDepth) {
        String key = regularKey(normalizedOwnerKey, budName);
        List<ConversationMemoryEntry> existing = new ArrayList<>(this.entriesByBud.getOrDefault(key, List.of()));
        existing.add(entry);
        this.entriesByBud.put(key, sortAndCap(normalizedOwnerKey, existing, maxDepth));
    }

    @Nullable
    ConversationMemoryEntry removeById(@Nonnull String normalizedOwnerKey, long id) {
        String ownerPrefix = normalizedOwnerKey + "::";
        for (Map.Entry<String, List<ConversationMemoryEntry>> mapEntry : this.entriesByBud.entrySet()) {
            if (!mapEntry.getKey().startsWith(ownerPrefix)) {
                continue;
            }
            List<ConversationMemoryEntry> bucket = Objects.requireNonNull(mapEntry.getValue());
            for (int i = 0; i < bucket.size(); i++) {
                if (bucket.get(i).id() == id) {
                    return bucket.remove(i);
                }
            }
        }
        return null;
    }

    void clearForOwner(@Nonnull String normalizedOwnerKey) {
        this.entriesByBud.keySet().removeIf(key -> key.startsWith(normalizedOwnerKey + "::"));
    }

    @Nonnull
    List<ConversationMemoryEntry> filterRelevant(@Nonnull String normalizedOwnerKey, @Nonnull Set<String> participants,
            int limit) {
        List<ConversationMemoryEntry> entries = collectForOwner(normalizedOwnerKey);
        if (entries.isEmpty()) {
            return Objects.requireNonNull(List.of());
        }
        return Objects.requireNonNull(entries.stream()
                .filter(entry -> !intersection(entry.participants(), participants).isEmpty())
                .sorted(Comparator
                        .comparingInt((ConversationMemoryEntry entry) -> intersection(entry.participants(),
                                participants).size())
                        .thenComparingDouble(entry -> entry.effectiveScore())
                        .thenComparingLong(entry -> entry.createdAt())
                        .reversed())
                .limit(Math.max(1, limit))
                .toList());
    }

    @Nonnull
    private List<ConversationMemoryEntry> collectForOwner(@Nonnull String normalizedOwnerKey) {
        List<ConversationMemoryEntry> combined = new ArrayList<>();
        String ownerPrefix = normalizedOwnerKey + "::";
        for (Map.Entry<String, List<ConversationMemoryEntry>> mapEntry : this.entriesByBud.entrySet()) {
            if (mapEntry.getKey().startsWith(ownerPrefix)) {
                combined.addAll(mapEntry.getValue());
            }
        }
        return combined;
    }

    @Nonnull
    private static List<ConversationMemoryEntry> sortAndCap(@Nonnull String normalizedOwnerKey,
            @Nonnull List<ConversationMemoryEntry> entries, int maxDepth) {
        List<ConversationMemoryEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator
                .comparingDouble((ConversationMemoryEntry entry) -> entry.effectiveScore())
                .thenComparingLong(entry -> entry.createdAt())
                .reversed());
        if (sorted.size() > maxDepth) {
            for (ConversationMemoryEntry evicted : sorted.subList(maxDepth, sorted.size())) {
                LoggerUtil.getLogger().info(() -> "[BUD] Memory evicted for player " + normalizedOwnerKey
                        + " (capacity " + maxDepth + " reached): " + evicted.summary());
            }
            return new ArrayList<>(sorted.subList(0, maxDepth));
        }
        return sorted;
    }

    @Nonnull
    private static Set<String> intersection(@Nonnull Set<String> left, @Nonnull Set<String> right) {
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return intersection;
    }

    @Nonnull
    private static String normalize(@Nonnull String participant) {
        return Objects.requireNonNull(participant.trim().toLowerCase());
    }
}
