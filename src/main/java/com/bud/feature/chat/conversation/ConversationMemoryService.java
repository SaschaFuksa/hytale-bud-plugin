package com.bud.feature.chat.conversation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.bud.core.config.ConversationConfig;
import com.bud.core.config.LLMConfig;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.queue.IQueueEntry;
import com.bud.llm.LLMCaller;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class ConversationMemoryService {

    @Nonnull
    private static final ConversationMemoryService INSTANCE = new ConversationMemoryService();
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("(?s)\\{.*\\}");
    private static final Pattern JSON_STRING_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"((?:\\\\.|[^\\\"])*)\"");
    private static final Pattern JSON_NUMBER_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(-?\\d+)");
    private static final Pattern JSON_BOOLEAN_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*(true|false)");
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
    private static final Pattern JSON_ARRAY_STRING_PATTERN = Pattern.compile("\"((?:\\\\.|[^\\\"])*)\"");

    private final Map<String, List<ConversationMemoryEntry>> memoriesByOwner = new ConcurrentHashMap<>();
    private final Map<String, List<ConversationMemoryEntry>> legendaryMemoriesByBud = new ConcurrentHashMap<>();
    private final Map<String, Object> ownerLocks = new ConcurrentHashMap<>();

    private ConversationMemoryService() {
    }

    @Nonnull
    public static ConversationMemoryService getInstance() {
        return Objects.requireNonNull(INSTANCE);
    }

    @Nonnull
    public Object getConversationLock(@Nonnull String ownerKey) {
        return Objects.requireNonNull(
                this.ownerLocks.computeIfAbsent(normalizeParticipant(ownerKey), ignored -> new Object()));
    }

    @Nonnull
    public Prompt augmentPrompt(@Nonnull Prompt prompt, @Nonnull ConversationContext context,
            @Nonnull String budName) {
        if (!ConversationConfig.getInstance().isEnableConversationMemory()) {
            return prompt;
        }

        List<ConversationMemoryEntry> relevantMemories = getRelevantMemories(context, budName);
        if (relevantMemories.isEmpty()) {
            return prompt;
        }

        StringBuilder userPromptBuilder = new StringBuilder(prompt.userPrompt().trim());
        userPromptBuilder.append("\n\nRelevant conversation memories:\n");
        for (ConversationMemoryEntry entry : relevantMemories) {
            userPromptBuilder.append("- ").append(entry.formatForPrompt()).append("\n");
        }
        userPromptBuilder.append("Use these memories only when they genuinely help. Do not repeat them verbatim.");
        return new Prompt(prompt.systemPrompt(), userPromptBuilder.toString());
    }

    public void afterInteraction(@Nonnull LLMInteractionEntry interactionEntry, @Nonnull IBudProfile budProfile,
            String message) {
        if (message == null || message.isBlank() || !ConversationConfig.getInstance().isEnableConversationMemory()
                || !LLMConfig.getInstance().isEnableLLM()) {
            return;
        }

        MemoryContext memoryContext = createMemoryContext(interactionEntry, budProfile);
        if (memoryContext == null) {
            return;
        }

        if (interactionEntry.promptContext() instanceof ConversationContext conversationContext
                && conversationContext.getConversationMode() == ConversationMode.DIALOG_MODE) {
            DialogModeTracker.getInstance().onDialogInteractionCompleted(conversationContext, budProfile, message);
        }

        SummaryCandidate summaryCandidate = summarizeResponse(memoryContext, budProfile, message);
        if (summaryCandidate == null) {
            LoggerUtil.getLogger().fine(() -> "[BUD] Memory candidate discarded for " + budProfile.getNPCDisplayName()
                    + " because no valid structured summary was produced.");
            return;
        }

        ConversationConfig config = ConversationConfig.getInstance();

        if (summaryCandidate.legendary() && config.isEnableLegendaryMemory()) {
            storeLegendaryMemory(memoryContext, budProfile, summaryCandidate);
            return;
        }

        if (summaryCandidate.importance() < config.getConversationMemoryMinImportance()) {
            LoggerUtil.getLogger().fine(() -> "[BUD] Memory candidate discarded for " + budProfile.getNPCDisplayName()
                    + " because importance " + summaryCandidate.importance()
                    + " is below threshold " + config.getConversationMemoryMinImportance() + ".");
            return;
        }

        String ownerKey = normalizeParticipant(memoryContext.ownerKey());
        synchronized (getConversationLock(ownerKey)) {
            List<ConversationMemoryEntry> existing = new ArrayList<>(
                    this.memoriesByOwner.getOrDefault(ownerKey, List.of()));
            List<ConversationMemoryEntry> decayed = new ArrayList<>(existing.size() + 1);
            for (ConversationMemoryEntry entry : existing) {
                decayed.add(entry.decay(config.getConversationMemoryDecayFactor()));
            }

            decayed.add(new ConversationMemoryEntry(
                    summaryCandidate.summary(),
                    summaryCandidate.importance(),
                    summaryCandidate.importance(),
                    budProfile.getNPCDisplayName(),
                    memoryContext.mode(),
                    buildStoredParticipants(summaryCandidate.participants(), budProfile.getNPCDisplayName()),
                    System.currentTimeMillis(),
                    false));

            decayed.sort(Comparator
                    .comparingDouble((ConversationMemoryEntry entry) -> entry.effectiveScore())
                    .thenComparingLong((ConversationMemoryEntry entry) -> entry.createdAt())
                    .reversed());

            int maxDepth = Math.max(1, config.getConversationMemoryDepth());
            if (decayed.size() > maxDepth) {
                List<ConversationMemoryEntry> evicted = decayed.subList(maxDepth, decayed.size());
                for (ConversationMemoryEntry entry : evicted) {
                    LoggerUtil.getLogger().info(() -> "[BUD] Memory evicted for player " + ownerKey
                            + " (capacity " + maxDepth + " reached): " + entry.summary());
                }
                decayed = new ArrayList<>(decayed.subList(0, maxDepth));
            }

            this.memoriesByOwner.put(ownerKey, decayed);
            LoggerUtil.getLogger().info(() -> "[BUD] Added memory for player " + ownerKey
                    + " from " + budProfile.getNPCDisplayName()
                    + " with importance " + summaryCandidate.importance()
                    + ": " + summaryCandidate.summary());
        }
    }

    @Nonnull
    public List<ConversationMemoryEntry> getMemoriesForOwner(@Nonnull String ownerKey) {
        String normalizedOwner = normalizeParticipant(ownerKey);
        List<ConversationMemoryEntry> entries = this.memoriesByOwner.getOrDefault(normalizedOwner, List.of());
        return Objects.requireNonNull(List.copyOf(entries));
    }

    @Nonnull
    public List<ConversationMemoryEntry> getLegendaryMemoriesForBud(@Nonnull String ownerKey,
            @Nonnull String budName) {
        String normalizedOwner = normalizeParticipant(ownerKey);
        List<ConversationMemoryEntry> entries = this.legendaryMemoriesByBud
                .getOrDefault(legendaryKey(normalizedOwner, budName), List.of());
        return Objects.requireNonNull(List.copyOf(entries));
    }

    public void clearPlayer(@Nonnull String ownerKey) {
        String normalizedOwner = normalizeParticipant(ownerKey);
        this.memoriesByOwner.remove(normalizedOwner);
        this.ownerLocks.remove(normalizedOwner);
        this.legendaryMemoriesByBud.keySet().removeIf(key -> key.startsWith(normalizedOwner + "::"));
    }

    @Nonnull
    private List<ConversationMemoryEntry> getRelevantMemories(@Nonnull ConversationContext context,
            @Nonnull String budName) {
        String ownerKey = normalizeParticipant(context.getConversationOwnerKey());
        List<ConversationMemoryEntry> entries = this.memoriesByOwner.getOrDefault(ownerKey, List.of());

        List<ConversationMemoryEntry> regular;
        if (entries.isEmpty()) {
            regular = List.of();
        } else {
            Set<String> participants = normalizeParticipants(context.getConversationParticipants());
            regular = entries.stream()
                    .filter(entry -> !intersection(entry.participants(), participants).isEmpty())
                    .sorted(Comparator
                            .comparingInt(
                                    (ConversationMemoryEntry entry) -> intersection(entry.participants(), participants)
                                            .size())
                            .thenComparingDouble((ConversationMemoryEntry entry) -> entry.effectiveScore())
                            .thenComparingLong((ConversationMemoryEntry entry) -> entry.createdAt())
                            .reversed())
                    .limit(Math.max(1, ConversationConfig.getInstance().getConversationMemoryDepth()))
                    .toList();
        }

        List<ConversationMemoryEntry> legendary = ConversationConfig.getInstance().isEnableLegendaryMemory()
                ? this.legendaryMemoriesByBud.getOrDefault(legendaryKey(ownerKey, budName), List.of())
                : List.of();

        if (legendary.isEmpty()) {
            return Objects.requireNonNull(regular);
        }

        List<ConversationMemoryEntry> combined = new ArrayList<>(legendary.size() + regular.size());
        combined.addAll(legendary);
        combined.addAll(regular);
        return Objects.requireNonNull(List.copyOf(combined));
    }

    private void storeLegendaryMemory(@Nonnull MemoryContext memoryContext, @Nonnull IBudProfile budProfile,
            @Nonnull SummaryCandidate candidate) {
        String ownerKey = normalizeParticipant(memoryContext.ownerKey());
        String bucketKey = legendaryKey(ownerKey, budProfile.getNPCDisplayName());
        int maxSlots = Math.max(1, ConversationConfig.getInstance().getLegendaryMemorySlotsPerBud());

        synchronized (getConversationLock(ownerKey)) {
            List<ConversationMemoryEntry> existing = new ArrayList<>(
                    this.legendaryMemoriesByBud.getOrDefault(bucketKey, List.of()));

            ConversationMemoryEntry candidateEntry = new ConversationMemoryEntry(
                    candidate.summary(),
                    candidate.importance(),
                    candidate.importance(),
                    budProfile.getNPCDisplayName(),
                    memoryContext.mode(),
                    buildStoredParticipants(candidate.participants(), budProfile.getNPCDisplayName()),
                    System.currentTimeMillis(),
                    true);

            if (existing.size() < maxSlots) {
                existing.add(candidateEntry);
                this.legendaryMemoriesByBud.put(bucketKey, existing);
                LoggerUtil.getLogger().info(() -> "[BUD] Added legendary memory for player " + ownerKey
                        + " from " + budProfile.getNPCDisplayName() + ": " + candidate.summary());
                return;
            }

            int replaceIndex = resolveLegendaryReplacement(existing, candidateEntry, budProfile);
            if (replaceIndex < 0 || replaceIndex >= existing.size()) {
                LoggerUtil.getLogger().info(() -> "[BUD] Legendary memory candidate discarded for "
                        + budProfile.getNPCDisplayName() + " (slots full, no replacement chosen): "
                        + candidate.summary());
                return;
            }

            String replacedSummary = existing.get(replaceIndex).summary();
            existing.set(replaceIndex, candidateEntry);
            this.legendaryMemoriesByBud.put(bucketKey, existing);
            LoggerUtil.getLogger().info(() -> "[BUD] Replaced legendary memory for " + budProfile.getNPCDisplayName()
                    + ": \"" + replacedSummary + "\" -> \"" + candidate.summary() + "\"");
        }
    }

    private int resolveLegendaryReplacement(@Nonnull List<ConversationMemoryEntry> existing,
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

            Matcher matcher = JSON_OBJECT_PATTERN.matcher(rawResponse);
            String json = matcher.find() ? matcher.group() : rawResponse.trim();
            Integer replaceIndex = extractJsonNumber(json, "replaceIndex");
            return replaceIndex == null ? -1 : replaceIndex;
        } catch (Exception exception) {
            LoggerUtil.getLogger().fine(() -> "[BUD] Could not resolve legendary replacement for "
                    + budProfile.getNPCDisplayName() + ": " + exception.getMessage());
            return -1;
        }
    }

    @Nonnull
    private String legendaryKey(@Nonnull String normalizedOwnerKey, @Nonnull String budName) {
        return normalizedOwnerKey + "::" + normalizeParticipant(budName);
    }

    private SummaryCandidate summarizeResponse(@Nonnull MemoryContext context, @Nonnull IBudProfile budProfile,
            @Nonnull String message) {
        try {
            LLMPromptManager promptManager = LLMPromptManager.getInstance();
            String systemPrompt = promptManager.getSystemPrompt("memorySummary");
            if (systemPrompt == null || systemPrompt.isBlank()) {
                LoggerUtil.getLogger().fine(() -> "[BUD] memorySummary prompt is missing. Skipping memory creation.");
                return null;
            }

            StringBuilder userPromptBuilder = new StringBuilder();
            userPromptBuilder.append("Conversation mode: ").append(context.mode()).append("\n")
                    .append("Interaction type: ").append(context.interactionType()).append("\n")
                    .append("Speaker: ").append(budProfile.getNPCDisplayName()).append("\n")
                    .append("Known participants: ").append(String.join(", ", context.participants()))
                    .append("\n")
                    .append("Current conversation input: ").append(context.input()).append("\n")
                    .append("Buddy response to compress:\n")
                    .append(message).append("\n")
                    .append("Return strict JSON only.");

            String rawResponse = LLMCaller.getInstance()
                    .callRawLLM(new Prompt(systemPrompt, userPromptBuilder.toString()))
                    .join();
            if (rawResponse == null || rawResponse.isBlank()) {
                return null;
            }
            return parseSummaryCandidate(rawResponse, context.participants());
        } catch (Exception exception) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Could not summarize conversation memory: " + exception.getMessage());
            return null;
        }
    }

    private SummaryCandidate parseSummaryCandidate(@Nonnull String rawResponse,
            @Nonnull Set<String> fallbackParticipants) {
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(rawResponse);
        String json = matcher.find() ? matcher.group() : rawResponse.trim();
        String summary = extractJsonString(json, "summary");
        Integer importance = extractJsonNumber(json, "importance");
        boolean legendary = extractJsonBoolean(json, "legendary");
        Set<String> participants = extractJsonArray(json, "participants");
        if (summary == null || summary.isBlank() || importance == null) {
            return null;
        }
        if (participants.isEmpty()) {
            participants = fallbackParticipants;
        }
        int boundedImportance = Math.max(0, Math.min(10, importance));
        return new SummaryCandidate(Objects.requireNonNull(summary.trim()), boundedImportance, legendary,
                participants);
    }

    private MemoryContext createMemoryContext(@Nonnull LLMInteractionEntry interactionEntry,
            @Nonnull IBudProfile budProfile) {
        IPromptContext promptContext = interactionEntry.promptContext();
        if (promptContext instanceof ConversationContext conversationContext) {
            return new MemoryContext(
                    normalizeParticipant(conversationContext.getConversationOwnerKey()),
                    conversationContext.getConversationMode(),
                    conversationContext.getConversationParticipants(),
                    conversationContext.getConversationInput(),
                    resolveInteractionType(promptContext));
        }

        String ownerKey = promptContext.getBudComponent().getPlayerRef().getUsername();
        Set<String> participants = Objects.requireNonNull(Set.of(ownerKey, budProfile.getNPCDisplayName()));
        return new MemoryContext(
                normalizeParticipant(ownerKey),
                ConversationMode.GENERAL,
                participants,
                buildFallbackInput(promptContext),
                resolveInteractionType(promptContext));
    }

    @Nonnull
    private String buildFallbackInput(@Nonnull IPromptContext promptContext) {
        if (promptContext instanceof IQueueEntry queueEntry) {
            return "Interaction entry: " + queueEntry.getEntryName();
        }
        return "Interaction entry: " + promptContext.getClass().getSimpleName();
    }

    @Nonnull
    private String resolveInteractionType(@Nonnull IPromptContext promptContext) {
        if (promptContext instanceof IQueueEntry queueEntry) {
            return queueEntry.getEntryName();
        }
        return Objects.requireNonNull(promptContext.getClass().getSimpleName());
    }

    private String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile(JSON_STRING_PATTERN.pattern().formatted(Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        String groupValue = matcher.group(1);
        if (groupValue == null) {
            return null;
        }
        return decodeJsonString(groupValue);
    }

    private Integer extractJsonNumber(String json, String key) {
        Pattern pattern = Pattern.compile(JSON_NUMBER_PATTERN.pattern().formatted(Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return Integer.valueOf(matcher.group(1));
    }

    private boolean extractJsonBoolean(String json, String key) {
        Pattern pattern = Pattern.compile(JSON_BOOLEAN_PATTERN.pattern().formatted(Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }

    @Nonnull
    private Set<String> extractJsonArray(String json, String key) {
        Pattern pattern = Pattern.compile(JSON_ARRAY_PATTERN.pattern().formatted(Pattern.quote(key)), Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            return Objects.requireNonNull(Set.of());
        }
        String arrayBody = matcher.group(1);
        if (arrayBody == null) {
            return Objects.requireNonNull(Set.of());
        }
        Matcher valueMatcher = JSON_ARRAY_STRING_PATTERN.matcher(arrayBody);
        Set<String> values = new HashSet<>();
        while (valueMatcher.find()) {
            String groupValue = valueMatcher.group(1);
            if (groupValue != null) {
                values.add(decodeJsonString(groupValue));
            }
        }
        return values;
    }

    @Nonnull
    private String decodeJsonString(@Nonnull String value) {
        return Objects.requireNonNull(value.replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim());
    }

    @Nonnull
    private Set<String> intersection(@Nonnull Set<String> left, @Nonnull Set<String> right) {
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return intersection;
    }

    @Nonnull
    private Set<String> normalizeParticipants(@Nonnull Set<String> participants) {
        Set<String> normalized = new HashSet<>();
        for (String participant : participants) {
            if (participant == null || participant.isBlank()) {
                continue;
            }
            normalized.add(normalizeParticipant(participant));
        }
        return normalized;
    }

    @Nonnull
    private Set<String> buildStoredParticipants(@Nonnull Set<String> participants, @Nonnull String speakerName) {
        Set<String> normalized = normalizeParticipants(participants);
        normalized.add(normalizeParticipant(speakerName));
        return normalized;
    }

    @Nonnull
    private String normalizeParticipant(@Nonnull String participant) {
        return Objects.requireNonNull(participant.trim().toLowerCase());
    }

    private record SummaryCandidate(@Nonnull String summary, int importance, boolean legendary,
            @Nonnull Set<String> participants) {
    }

    private record MemoryContext(
            @Nonnull String ownerKey,
            @Nonnull ConversationMode mode,
            @Nonnull Set<String> participants,
            @Nonnull String input,
            @Nonnull String interactionType) {
    }
}