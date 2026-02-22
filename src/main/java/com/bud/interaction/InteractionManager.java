package com.bud.interaction;

import javax.annotation.Nonnull;

import com.bud.components.BudComponent;
import com.bud.config.LLMConfig;
import com.bud.events.ChatEvent;
import com.bud.events.SoundEvent;
import com.bud.llm.AbstractLLMMessageCreation;
import com.bud.llm.LLMCaller;
import com.bud.llm.messages.IPromptContext;
import com.bud.llm.messages.Prompt;
import com.bud.profile.IBudProfile;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class InteractionManager {

    private InteractionManager() {
    }

    private static final InteractionManager INSTANCE = new InteractionManager();

    public static InteractionManager getInstance() {
        return INSTANCE;
    }

    public void processInteraction(@Nonnull AbstractLLMMessageCreation llmMessageCreation,
            @Nonnull IPromptContext promptContext, @Nonnull BudComponent budComponent,
            @Nonnull IBudProfile budProfile) {
        Ref<EntityStore> entityRef = budComponent.getBud().getReference();
        if (entityRef == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Entity reference is null for Bud: " + budComponent.getBud());
            return;
        }
        Prompt prompt = llmMessageCreation.createPrompt(promptContext);
        if (prompt == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] No prompt found for: " + budProfile.getNPCDisplayName());
            return;
        }
        String message;
        if (LLMConfig.getInstance().isEnableLLM()) {
            message = LLMCaller.getInstance().callLLM(prompt, budProfile).join();
        } else {
            message = prompt.systemPrompt();
        }
        if (message == null || message.isBlank()) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] LLM returned empty message for: " + budProfile.getNPCDisplayName());
            return;
        }
        ChatEvent.dispatch(budComponent.getPlayerRef(), message);
        SoundEvent.dispatch(entityRef, budProfile.getBudSoundData().getPassiveSound());
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Processing interaction for: " + budProfile.getNPCDisplayName());
    }

}
