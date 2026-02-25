package com.bud.feature;

import javax.annotation.Nonnull;

import com.bud.core.config.LLMConfig;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.sound.SoundEvent;
import com.bud.llm.LLMCaller;
import com.bud.llm.prompt.Prompt;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class LLMInteractionManager {

    private LLMInteractionManager() {
    }

    private static final LLMInteractionManager INSTANCE = new LLMInteractionManager();

    public static LLMInteractionManager getInstance() {
        return INSTANCE;
    }

    public void processInteraction(@Nonnull LLMInteractionEntry interactionEntry) {
        Ref<EntityStore> entityRef = interactionEntry.budComponent().getBud().getReference();
        if (entityRef == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Entity reference is null for Bud: "
                            + interactionEntry.budComponent().getBud());
            return;
        }
        Prompt prompt = interactionEntry.llmMessageCreation().createPrompt(interactionEntry.promptContext());
        if (prompt == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No prompt found for: " + interactionEntry.budComponent().getBud());
            return;
        }
        String message;
        if (LLMConfig.getInstance().isEnableLLM()) {
            message = LLMCaller.getInstance().callLLM(prompt, interactionEntry.getBudProfile()).join();
        } else {
            message = prompt.systemPrompt();
        }
        if (message == null || message.isBlank()) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] LLM returned empty message for: " + interactionEntry.budComponent().getBud());
            return;
        }
        ChatEvent.dispatch(interactionEntry.budComponent().getPlayerRef(), message);
        SoundEvent.dispatch(entityRef, interactionEntry.getBudProfile().getBudSoundData().getPassiveSound());
        LoggerUtil.getLogger().fine(() -> "[BUD] Processing interaction for: "
                + interactionEntry.budComponent().getBud().getNPCTypeId());
    }

}
