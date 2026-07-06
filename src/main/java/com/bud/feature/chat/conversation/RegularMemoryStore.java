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

    private final Map<String, List<ConversationMemoryEntry>> entriesByOwner = new ConcurrentHashMap<>();

    @Nonnull
    List<ConversationMemoryEntry> getForOwner(@Nonnull String normalizedOwnerKey) {
        return Objects.requireNonNull(List.copyOf(this.entriesByOwner.getOrDefault(normalizedOwnerKey, List.of())));
    }

    void restoreForOwner(@Nonnull String normalizedOwnerKey, @Nonnull List<ConversationMemoryEntry> memories) {
        if (!memories.isEmpty()) {
            this.entriesByOwner.put(normalizedOwnerKey, new ArrayList<>(memories));
        }
    }

    void addDecayedAndNew(@Nonnull String normalizedOwnerKey, @Nonnull ConversationMemoryEntry newEntry,
            double decayFactor, int maxDepth) {
        List<ConversationMemoryEntry> existing = new ArrayList<>(
                this.entriesByOwner.getOrDefault(normalizedOwnerKey, List.of()));
        List<ConversationMemoryEntry> decayed = new ArrayList<>(existing.size() + 1);
        for (ConversationMemoryEntry entry : existing) {
            decayed.add(entry.decay(decayFactor));
        }
        decayed.add(newEntry);
        this.entriesByOwner.put(normalizedOwnerKey, sortAndCap(normalizedOwnerKey, decayed, maxDepth));
        LoggerUtil.getLogger().info(() -> "[BUD] Added memory for player " + normalizedOwnerKey
                + " from " + newEntry.speakerName()
                + " with importance " + newEntry.importance()
                + ": " + newEntry.summary());
    }

    void addManual(@Nonnull String normalizedOwnerKey, @Nonnull ConversationMemoryEntry entry, int maxDepth) {
        List<ConversationMemoryEntry> existing = new ArrayList<>(
                this.entriesByOwner.getOrDefault(normalizedOwnerKey, List.of()));
        existing.add(entry);
        this.entriesByOwner.put(normalizedOwnerKey, sortAndCap(normalizedOwnerKey, existing, maxDepth));
    }

    @Nullable
    ConversationMemoryEntry removeAt(@Nonnull String normalizedOwnerKey, int displayIndex) {
        List<ConversationMemoryEntry> existing = new ArrayList<>(
                this.entriesByOwner.getOrDefault(normalizedOwnerKey, List.of()));
        if (displayIndex < 1 || displayIndex > existing.size()) {
            return null;
        }
        ConversationMemoryEntry removed = existing.remove(displayIndex - 1);
        this.entriesByOwner.put(normalizedOwnerKey, existing);
        return removed;
    }

    void clearForOwner(@Nonnull String normalizedOwnerKey) {
        this.entriesByOwner.remove(normalizedOwnerKey);
    }

    @Nonnull
    List<ConversationMemoryEntry> filterRelevant(@Nonnull String normalizedOwnerKey, @Nonnull Set<String> participants,
            int limit) {
        List<ConversationMemoryEntry> entries = this.entriesByOwner.getOrDefault(normalizedOwnerKey, List.of());
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
}
