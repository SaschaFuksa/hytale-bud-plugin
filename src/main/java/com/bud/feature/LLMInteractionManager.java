package com.bud.feature;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.bud.core.config.LLMConfig;
import com.bud.core.registry.BudDefinition;
import com.bud.core.registry.BudRegistry;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.chat.conversation.ConversationContext;
import com.bud.feature.chat.conversation.ConversationMemoryService;
import com.bud.feature.sound.SoundEvent;
import com.bud.llm.LLMCaller;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.llm.prompt.Prompt;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class LLMInteractionManager {

    private LLMInteractionManager() {
    }

    private static final LLMInteractionManager INSTANCE = new LLMInteractionManager();

    public static LLMInteractionManager getInstance() {
        return INSTANCE;
    }

    public String processInteraction(@Nonnull LLMInteractionEntry interactionEntry) {
        if (interactionEntry.promptContext() instanceof ConversationContext conversationContext) {
            synchronized (ConversationMemoryService.getInstance()
                    .getConversationLock(conversationContext.getConversationOwnerKey())) {
                return processInteractionInternal(interactionEntry, conversationContext);
            }
        }
        return processInteractionInternal(interactionEntry, null);
    }

    private String processInteractionInternal(@Nonnull LLMInteractionEntry interactionEntry,
            ConversationContext conversationContext) {
        Ref<EntityStore> entityRef = interactionEntry.getBudComponent().getBud().getReference();
        BudDefinition budProfile = BudRegistry.getInstance().get(interactionEntry.getBudComponent().getBudId());
        String message = null;
        try {
            if (entityRef == null) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Entity reference is null for Bud: "
                                + interactionEntry.getBudComponent().getBud());
                return null;
            }
            Prompt prompt = interactionEntry.llmMessageCreation().createPrompt(interactionEntry.promptContext());
            if (prompt == null) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] No prompt found for: " + interactionEntry.getBudComponent().getBud());
                return null;
            }
            if (conversationContext != null) {
                prompt = ConversationMemoryService.getInstance().augmentPrompt(prompt, conversationContext,
                        budProfile.getDisplayName());
            }
            if (LLMConfig.getInstance().isEnableLLM()) {
                message = LLMCaller.getInstance().callLLM(prompt, budProfile).join();
            } else {
                message = prompt.systemPrompt();
            }
            if (message == null || message.isBlank()) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] LLM returned empty message for: "
                                + interactionEntry.getBudComponent().getBud());
                return null;
            }
            ChatEvent.dispatch(interactionEntry.getBudComponent().getPlayerRef(),
                    formatBudSpeech(budProfile, message));
            SoundEvent.dispatch(entityRef, budProfile.getSounds().getPassive());
            LoggerUtil.getLogger().fine(() -> "[BUD] Processing interaction for: "
                    + interactionEntry.getBudComponent().getBud().getNPCTypeId());
            return message;
        } finally {
            ConversationMemoryService.getInstance().afterInteraction(interactionEntry, budProfile, message);
        }
    }

    @Nonnull
    private Message formatBudSpeech(@Nonnull BudDefinition budProfile, @Nonnull String message) {
        String trimmedMessage = Objects.requireNonNull(message.trim());
        Message prefix = Message.raw(budProfile.getDisplayName() + ": ").color(budProfile.getColor());
        return Objects.requireNonNull(Message.join(prefix, Message.raw(trimmedMessage)));
    }

}
