package com.bud.feature.chat.conversation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.config.ConversationConfig;
import com.bud.core.config.LLMConfig;
import com.bud.feature.LLMPromptManager;
import com.bud.feature.bud.reaction.BudReactionEntry;
import com.bud.feature.bud.reaction.BudReactionKind;
import com.bud.feature.bud.reaction.LLMBudReactionMessageCreation;
import com.bud.feature.queue.IQueueEntry;
import com.bud.feature.queue.orchestrator.Orchestrator;
import com.bud.feature.queue.orchestrator.OrchestratorChannel;
import com.bud.feature.queue.orchestrator.OrchestratorQueue;
import com.bud.llm.LLMCaller;
import com.bud.llm.client.JsonUtils;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.llm.profiles.IBudProfile;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ConversationMemoryService {

    @Nonnull
    private static final ConversationMemoryService INSTANCE = new ConversationMemoryService();

    private final Map<String, Object> ownerLocks = new ConcurrentHashMap<>();
    private final LegendaryMemoryStore legendaryStore = new LegendaryMemoryStore();
    private final RegularMemoryStore regularStore = new RegularMemoryStore();
    private final Map<String, AtomicLong> nextMemoryIdByOwner = new ConcurrentHashMap<>();

    private ConversationMemoryService() {
    }

    /**
     * Regular and legendary memories share this sequence so no two memories for the
     * same player can ever collide, regardless of type.
     */
    private long allocateMemoryId(@Nonnull String normalizedOwnerKey) {
        return this.nextMemoryIdByOwner.computeIfAbsent(normalizedOwnerKey, k -> new AtomicLong(1)).getAndIncrement();
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
            storeLegendaryMemory(memoryContext, budProfile, summaryCandidate, interactionEntry.promptContext());
            return;
        }

        if (summaryCandidate.importance() < config.getConversationMemoryMinImportance()) {
            LoggerUtil.getLogger().fine(() -> "[BUD] Memory candidate discarded for " + budProfile.getNPCDisplayName()
                    + " because importance " + summaryCandidate.importance()
                    + " is below threshold " + config.getConversationMemoryMinImportance() + ".");
            return;
        }

        String ownerKey = normalizeParticipant(memoryContext.ownerKey());
        ConversationMemoryEntry newEntry = new ConversationMemoryEntry(
                allocateMemoryId(ownerKey),
                summaryCandidate.summary(),
                summaryCandidate.importance(),
                summaryCandidate.importance(),
                budProfile.getNPCDisplayName(),
                memoryContext.mode(),
                buildStoredParticipants(summaryCandidate.participants(), budProfile.getNPCDisplayName()),
                System.currentTimeMillis(),
                false);
        int maxDepth = Math.max(1, config.getConversationMemoryDepth());
        synchronized (getConversationLock(ownerKey)) {
            this.regularStore.addDecayedAndNew(ownerKey, budProfile.getNPCDisplayName(), newEntry,
                    config.getConversationMemoryDecayFactor(), config.getConversationMemoryMinImportance(), maxDepth);
        }
        persistOwnerMemories(ownerKey, interactionEntry.promptContext().getBudComponent().getPlayerRef());
    }

    private void persistOwnerMemories(@Nonnull String normalizedOwnerKey, @Nonnull PlayerRef playerRef) {
        long nextMemoryId = this.nextMemoryIdByOwner
                .computeIfAbsent(normalizedOwnerKey, k -> new AtomicLong(1)).get();
        ConversationMemoryPersistence.persist(normalizedOwnerKey, playerRef,
                this.regularStore.getForOwner(normalizedOwnerKey),
                this.legendaryStore.snapshotForOwner(normalizedOwnerKey), nextMemoryId);
    }

    @Nonnull
    public List<ConversationMemoryEntry> getMemoriesForOwner(@Nonnull String ownerKey) {
        String normalizedOwner = normalizeParticipant(ownerKey);
        return this.regularStore.getForOwner(normalizedOwner);
    }

    @Nonnull
    public List<ConversationMemoryEntry> getLegendaryMemoriesForBud(@Nonnull String ownerKey,
            @Nonnull String budName) {
        String normalizedOwner = normalizeParticipant(ownerKey);
        return this.legendaryStore.collectForBud(normalizedOwner, budName);
    }

    public void restoreForOwner(@Nonnull String ownerKey, @Nonnull PlayerBudComponent component) {
        String normalizedOwner = normalizeParticipant(ownerKey);

        List<ConversationMemoryEntry> memories = ConversationMemoryPersistence.restoreRegularMemories(component);
        this.regularStore.restoreForOwner(normalizedOwner, memories);

        Map<String, List<ConversationMemoryEntry>> legendaryBuckets = ConversationMemoryPersistence
                .restoreLegendaryBuckets(component);
        this.legendaryStore.restoreBuckets(legendaryBuckets);

        long persistedNextId = ConversationMemoryPersistence.restoreNextMemoryId(component);
        this.nextMemoryIdByOwner.put(normalizedOwner, new AtomicLong(Math.max(1, persistedNextId)));

        LoggerUtil.getLogger().fine(() -> "[BUD] Restored " + memories.size()
                + " memories and " + legendaryBuckets.size()
                + " legendary memory buckets for player " + normalizedOwner);
    }

    public void addManualMemory(@Nonnull String ownerKey, @Nonnull PlayerRef playerRef,
            @Nonnull String budDisplayName, @Nonnull String summary) {
        String normalizedOwner = normalizeParticipant(ownerKey);
        ConversationMemoryEntry entry = new ConversationMemoryEntry(
                allocateMemoryId(normalizedOwner),
                summary, 10, 10, budDisplayName, ConversationMode.GENERAL,
                buildStoredParticipants(Objects.requireNonNull(Set.of(normalizedOwner)), budDisplayName),
                System.currentTimeMillis(), false);

        int maxDepth = Math.max(1, ConversationConfig.getInstance().getConversationMemoryDepth());
        synchronized (getConversationLock(normalizedOwner)) {
            this.regularStore.addManual(normalizedOwner, budDisplayName, entry, maxDepth);
        }
        persistOwnerMemories(normalizedOwner, playerRef);
    }

    @Nullable
    public ConversationMemoryEntry removeMemoryAt(@Nonnull String ownerKey, @Nonnull PlayerRef playerRef,
            long id) {
        String normalizedOwner = normalizeParticipant(ownerKey);
        ConversationMemoryEntry removed;
        synchronized (getConversationLock(normalizedOwner)) {
            removed = this.regularStore.removeById(normalizedOwner, id);
        }
        if (removed != null) {
            persistOwnerMemories(normalizedOwner, playerRef);
        }
        return removed;
    }

    public boolean addManualLegendaryMemory(@Nonnull String ownerKey, @Nonnull PlayerRef playerRef,
            @Nonnull String budDisplayName, @Nonnull String summary) {
        String normalizedOwner = normalizeParticipant(ownerKey);
        ConversationMemoryEntry entry = new ConversationMemoryEntry(
                allocateMemoryId(normalizedOwner),
                summary, 10, 10, budDisplayName, ConversationMode.GENERAL,
                buildStoredParticipants(Objects.requireNonNull(Set.of(normalizedOwner)), budDisplayName),
                System.currentTimeMillis(), true);
        int maxSlots = Math.max(1, ConversationConfig.getInstance().getLegendaryMemorySlotsPerBud());
        String bucketKey = this.legendaryStore.legendaryKey(normalizedOwner, budDisplayName);

        boolean stored;
        synchronized (getConversationLock(normalizedOwner)) {
            stored = this.legendaryStore.storeOrReplace(normalizedOwner, bucketKey, entry, maxSlots, budDisplayName);
        }
        if (stored) {
            persistOwnerMemories(normalizedOwner, playerRef);
        }
        return stored;
    }

    public boolean removeLegendaryMemoryAt(@Nonnull String ownerKey, @Nonnull PlayerRef playerRef,
            @Nonnull String budDisplayName, long id) {
        String normalizedOwner = normalizeParticipant(ownerKey);
        boolean removed;
        synchronized (getConversationLock(normalizedOwner)) {
            removed = this.legendaryStore.removeById(normalizedOwner, budDisplayName, id);
        }
        if (removed) {
            persistOwnerMemories(normalizedOwner, playerRef);
        }
        return removed;
    }

    public void clearPlayer(@Nonnull String ownerKey) {
        String normalizedOwner = normalizeParticipant(ownerKey);
        this.regularStore.clearForOwner(normalizedOwner);
        this.ownerLocks.remove(normalizedOwner);
        this.legendaryStore.clearForOwner(normalizedOwner);
        this.nextMemoryIdByOwner.remove(normalizedOwner);
    }

    @Nonnull
    private List<ConversationMemoryEntry> getRelevantMemories(@Nonnull ConversationContext context,
            @Nonnull String budName) {
        String ownerKey = normalizeParticipant(context.getConversationOwnerKey());
        Set<String> participants = normalizeParticipants(context.getConversationParticipants());
        int limit = Math.max(1, ConversationConfig.getInstance().getConversationMemoryDepth());
        List<ConversationMemoryEntry> regular = this.regularStore.filterRelevant(ownerKey, participants, limit);

        List<ConversationMemoryEntry> legendary = ConversationConfig.getInstance().isEnableLegendaryMemory()
                ? this.legendaryStore.collectForBud(ownerKey, budName)
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
            @Nonnull SummaryCandidate candidate, @Nonnull IPromptContext promptContext) {
        String ownerKey = normalizeParticipant(memoryContext.ownerKey());
        String contextPairKey = memoryContext.pairKey();
        String bucketKey = contextPairKey != null ? contextPairKey
                : this.legendaryStore.legendaryKey(ownerKey, budProfile.getNPCDisplayName());
        int maxSlots = Math.max(1, ConversationConfig.getInstance().getLegendaryMemorySlotsPerBud());

        ConversationMemoryEntry candidateEntry = new ConversationMemoryEntry(
                allocateMemoryId(ownerKey),
                candidate.summary(),
                candidate.importance(),
                candidate.importance(),
                budProfile.getNPCDisplayName(),
                memoryContext.mode(),
                buildStoredParticipants(candidate.participants(), budProfile.getNPCDisplayName()),
                System.currentTimeMillis(),
                true);

        boolean stored;
        synchronized (getConversationLock(ownerKey)) {
            stored = this.legendaryStore.storeOrReplace(ownerKey, bucketKey, candidateEntry, maxSlots,
                    budProfile.getNPCDisplayName());
        }

        if (stored) {
            persistOwnerMemories(ownerKey, promptContext.getBudComponent().getPlayerRef());
            triggerLegendaryReaction(promptContext, budProfile, candidateEntry);
        }
    }

    private void triggerLegendaryReaction(@Nonnull IPromptContext promptContext, @Nonnull IBudProfile budProfile,
            @Nonnull ConversationMemoryEntry candidateEntry) {
        try {
            PlayerRef playerRef = promptContext.getBudComponent().getPlayerRef();
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) {
                return;
            }
            Store<EntityStore> store = ref.getStore();
            BudComponent speakerBud = promptContext.getBudComponent();

            store.getExternalData().getWorld().execute(() -> {
                PlayerBudComponent playerBudComponent = store.getComponent(ref, PlayerBudComponent.getComponentType());
                if (playerBudComponent == null) {
                    return;
                }
                BudComponent otherBud = BudManager.getInstance().getRandomOtherBud(playerBudComponent, speakerBud);
                if (otherBud == null) {
                    return;
                }
                String situationInfo = budProfile.getNPCDisplayName() + " just had a defining moment: \""
                        + candidateEntry.summary() + "\". React to this in character.";
                BudReactionEntry entry = new BudReactionEntry(otherBud, BudReactionKind.LEGENDARY_MEMORY,
                        situationInfo);
                long now = System.currentTimeMillis();
                Orchestrator.getInstance().enqueue(new OrchestratorQueue(
                        OrchestratorChannel.SOCIAL,
                        entry,
                        entry.getEntryName() + ":" + now,
                        playerRef.getUsername(),
                        new LLMInteractionEntry(LLMBudReactionMessageCreation.getInstance(), entry),
                        now));
            });
        } catch (Exception exception) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Could not trigger legendary memory reaction: "
                    + exception.getMessage());
        }
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
        String json = JsonUtils.extractJsonObject(rawResponse);
        String summary = JsonUtils.extractString(json, "summary");
        Integer importance = JsonUtils.extractInt(json, "importance");
        boolean legendary = JsonUtils.extractBoolean(json, "legendary");
        Set<String> participants = JsonUtils.extractStringArray(json, "participants");
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
            String normalizedOwnerKey = normalizeParticipant(conversationContext.getConversationOwnerKey());
            String pairKey = null;
            if (promptContext instanceof DialogEntry(var _, var _, String previousSpeakerName, var _, var _)
                    && previousSpeakerName != null && !previousSpeakerName.isBlank()) {
                pairKey = this.legendaryStore.pairKey(normalizedOwnerKey, previousSpeakerName,
                        budProfile.getNPCDisplayName());
            }
            return new MemoryContext(
                    normalizedOwnerKey,
                    conversationContext.getConversationMode(),
                    conversationContext.getConversationParticipants(),
                    conversationContext.getConversationInput(),
                    resolveInteractionType(promptContext),
                    pairKey);
        }

        String ownerKey = promptContext.getBudComponent().getPlayerRef().getUsername();
        Set<String> participants = Objects.requireNonNull(Set.of(ownerKey, budProfile.getNPCDisplayName()));
        return new MemoryContext(
                normalizeParticipant(ownerKey),
                ConversationMode.GENERAL,
                participants,
                buildFallbackInput(promptContext),
                resolveInteractionType(promptContext),
                null);
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
            @Nonnull String interactionType,
            String pairKey) {
    }
}
