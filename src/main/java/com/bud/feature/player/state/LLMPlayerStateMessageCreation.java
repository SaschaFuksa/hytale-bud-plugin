package com.bud.feature.player.state;

import javax.annotation.Nonnull;

import com.bud.core.types.Mood;
import com.bud.feature.LLMPromptManager;
import com.bud.llm.messages.AbstractLLMMessageCreation;
import com.bud.llm.messages.BudMessage;
import com.bud.llm.prompt.IPromptContext;
import com.bud.llm.prompt.Prompt;

public class LLMPlayerStateMessageCreation extends AbstractLLMMessageCreation {

    @Nonnull
    private static final LLMPlayerStateMessageCreation INSTANCE = new LLMPlayerStateMessageCreation();

    private LLMPlayerStateMessageCreation() {
    }

    @Nonnull
    public static LLMPlayerStateMessageCreation getInstance() {
        return INSTANCE;
    }

    @Override
    protected Prompt createLLMPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof PlayerStateEntry playerStateEntry)) {
            throw new IllegalArgumentException("Context must be of type PlayerStateEntry");
        }
        BudMessage npcMessage = playerStateEntry.getBudProfile().getBudMessage();

        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budInfo = npcMessage.getCharacteristics();
        String stateInfo = playerStateEntry.getStateInformation();
        String stateView = npcMessage.getPlayerStateView(playerStateEntry.debuff());

        StringBuilder systemPromptBuilder = new StringBuilder();
        systemPromptBuilder.append(manager.getSystemPrompt("playerState")).append("\n")
                .append(manager.getSystemPrompt("default")).append("\n")
                .append(budInfo).append("\n").append(stateView);

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(stateInfo).append("\n")
                .append(manager.getSystemPrompt("final"));

        if (!playerStateEntry.budComponent().getCurrentMood().equals(Mood.DEFAULT)) {
            systemPromptBuilder.append("\n").append(manager.getMoodPrompt("instruction"));
            systemPromptBuilder.append("\n")
                    .append(manager.getMoodPrompt(
                            playerStateEntry.budComponent().getCurrentMood().getDisplayName().toLowerCase()));
            messageBuilder.append("\n").append(manager.getSystemPrompt("final-mood"));
        }

        String systemPrompt = systemPromptBuilder.toString();
        String message = messageBuilder.toString();

        return new Prompt(systemPrompt, message);
    }

    @Override
    protected Prompt createFallbackPrompt(@Nonnull IPromptContext context) {
        if (!(context instanceof PlayerStateEntry playerStateEntry)) {
            throw new IllegalArgumentException("Context must be of type PlayerStateEntry");
        }
        String message = playerStateEntry.getBudProfile().getBudMessage().getFallback("default");
        return new Prompt(message, message);
    }
}
