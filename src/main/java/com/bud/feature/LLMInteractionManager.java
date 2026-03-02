package com.bud.feature;

import javax.annotation.Nonnull;

import com.bud.core.config.LLMConfig;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.profiles.BudProfileMapper;
import com.bud.feature.sound.SoundEvent;
import com.bud.llm.LLMCaller;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.llm.profiles.IBudProfile;
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
        Ref<EntityStore> entityRef = interactionEntry.getBudComponent().getBud().getReference();
        IBudProfile budProfile = BudProfileMapper.getInstance()
                .getProfileForBudType(interactionEntry.getBudComponent().getBudType());
        if (entityRef == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] Entity reference is null for Bud: "
                            + interactionEntry.getBudComponent().getBud());
            return;
        }
        Prompt prompt = interactionEntry.llmMessageCreation().createPrompt(interactionEntry.promptContext());
        if (prompt == null) {
            LoggerUtil.getLogger()
                    .warning(() -> "[BUD] No prompt found for: " + interactionEntry.getBudComponent().getBud());
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
                    .warning(() -> "[BUD] LLM returned empty message for: "
                            + interactionEntry.getBudComponent().getBud());
            return;
        }
        ChatEvent.dispatch(interactionEntry.getBudComponent().getPlayerRef(), message);
        SoundEvent.dispatch(entityRef, budProfile.getBudSoundData().getPassiveSound());
        LoggerUtil.getLogger().fine(() -> "[BUD] Processing interaction for: "
                + interactionEntry.getBudComponent().getBud().getNPCTypeId());
    }

}
