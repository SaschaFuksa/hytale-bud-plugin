package com.bud.feature.chat.conversation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import com.bud.feature.LLMPromptManager;
import com.bud.llm.LLMCaller;
import com.bud.llm.client.JsonUtils;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.Prompt;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

/**
 * Owns the legendary-memory buckets keyed either by a single bud (player-bud memories)
 * or by a sorted bud pair (bud-2-bud memories, see {@link #pairKey}).
 */
final class LegendaryMemoryStore {

    private final Map<String, List<ConversationMemoryEntry>> memoriesByKey = new ConcurrentHashMap<>();

    @Nonnull
    String legendaryKey(@Nonnull String normalizedOwnerKey, @Nonnull String budName) {
        return normalizedOwnerKey + "::" + normalize(budName);
    }

    @Nonnull
    String pairKey(@Nonnull String normalizedOwnerKey, @Nonnull String budNameA, @Nonnull String budNameB) {
        String a = normalize(budNameA);
        String b = normalize(budNameB);
        String sortedPair = a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
        return normalizedOwnerKey + "::" + sortedPair;
    }

    @Nonnull
    List<ConversationMemoryEntry> collectForBud(@Nonnull String normalizedOwnerKey, @Nonnull String budName) {
        String normalizedBudName = normalize(budName);
        List<ConversationMemoryEntry> combined = new ArrayList<>(
                this.memoriesByKey.getOrDefault(legendaryKey(normalizedOwnerKey, budName), List.of()));

        String ownerPrefix = normalizedOwnerKey + "::";
        for (Map.Entry<String, List<ConversationMemoryEntry>> mapEntry : this.memoriesByKey.entrySet()) {
            String key = mapEntry.getKey();
            if (!key.startsWith(ownerPrefix) || !key.contains("|")) {
                continue;
            }
            String[] pairNames = key.substring(ownerPrefix.length()).split("\\|", 2);
            if (pairNames.length == 2
                    && (pairNames[0].equals(normalizedBudName) || pairNames[1].equals(normalizedBudName))) {
                combined.addAll(mapEntry.getValue());
            }
        }
        return Objects.requireNonNull(List.copyOf(combined));
    }

    /**
     * Adds the candidate to the bucket if a slot is free, otherwise asks the LLM to pick a
     * replacement. Callers are expected to hold the owner's conversation lock.
     */
    boolean storeOrReplace(@Nonnull String ownerKey, @Nonnull String bucketKey,
            @Nonnull ConversationMemoryEntry candidateEntry, int maxSlots, @Nonnull IBudProfile budProfile) {
        List<ConversationMemoryEntry> existing = new ArrayList<>(
                this.memoriesByKey.getOrDefault(bucketKey, List.of()));

        if (existing.size() < maxSlots) {
            existing.add(candidateEntry);
            this.memoriesByKey.put(bucketKey, existing);
            LoggerUtil.getLogger().info(() -> "[BUD] Added legendary memory for player " + ownerKey
                    + " from " + budProfile.getNPCDisplayName() + ": " + candidateEntry.summary());
            return true;
        }

        int replaceIndex = resolveReplacement(existing, candidateEntry, budProfile);
        if (replaceIndex < 0 || replaceIndex >= existing.size()) {
            LoggerUtil.getLogger().info(() -> "[BUD] Legendary memory candidate discarded for "
                    + budProfile.getNPCDisplayName() + " (slots full, no replacement chosen): "
                    + candidateEntry.summary());
            return false;
        }

        String replacedSummary = existing.get(replaceIndex).summary();
        existing.set(replaceIndex, candidateEntry);
        this.memoriesByKey.put(bucketKey, existing);
        LoggerUtil.getLogger().info(() -> "[BUD] Replaced legendary memory for " + budProfile.getNPCDisplayName()
                + ": \"" + replacedSummary + "\" -> \"" + candidateEntry.summary() + "\"");
        return true;
    }

    @Nonnull
    Map<String, List<ConversationMemoryEntry>> snapshotForOwner(@Nonnull String normalizedOwnerKey) {
        Map<String, List<ConversationMemoryEntry>> snapshot = new HashMap<>();
        String ownerPrefix = normalizedOwnerKey + "::";
        for (Map.Entry<String, List<ConversationMemoryEntry>> entry : this.memoriesByKey.entrySet()) {
            if (entry.getKey().startsWith(ownerPrefix)) {
                snapshot.put(entry.getKey(), entry.getValue());
            }
        }
        return snapshot;
    }

    void restoreBuckets(@Nonnull Map<String, List<ConversationMemoryEntry>> buckets) {
        this.memoriesByKey.putAll(buckets);
    }

    void clearForOwner(@Nonnull String normalizedOwnerKey) {
        this.memoriesByKey.keySet().removeIf(key -> key.startsWith(normalizedOwnerKey + "::"));
    }

    private int resolveReplacement(@Nonnull List<ConversationMemoryEntry> existing,
            @Nonnull ConversationMemoryEntry candidate, @Nonnull IBudProfile budProfile) {
        try {
            LLMPromptManager promptManager = LLMPromptManager.getInstance();
            String systemPrompt = promptManager.getSystemPrompt("legendaryReplacement");
            if (systemPrompt == null || systemPrompt.isBlank()) {
                return -1;
            }

            StringBuilder userPromptBuilder = new StringBuilder();
            userPromptBuilder.append("Existing legendary memories:\n");
            for (int i = 0; i < existing.size(); i++) {
                userPromptBuilder.append(i).append(": ").append(existing.get(i).summary()).append("\n");
            }
            userPromptBuilder.append("New candidate memory: ").append(candidate.summary()).append("\n")
                    .append("Return strict JSON only.");

            String rawResponse = LLMCaller.getInstance()
                    .callRawLLM(new Prompt(systemPrompt, userPromptBuilder.toString()))
                    .join();
            if (rawResponse == null || rawResponse.isBlank()) {
                return -1;
            }

            String json = JsonUtils.extractJsonObject(rawResponse);
            Integer replaceIndex = JsonUtils.extractInt(json, "replaceIndex");
            return replaceIndex == null ? -1 : replaceIndex;
        } catch (Exception exception) {
            LoggerUtil.getLogger().fine(() -> "[BUD] Could not resolve legendary replacement for "
                    + budProfile.getNPCDisplayName() + ": " + exception.getMessage());
            return -1;
        }
    }

    @Nonnull
    private static String normalize(@Nonnull String participant) {
        return Objects.requireNonNull(participant.trim().toLowerCase());
    }
}
