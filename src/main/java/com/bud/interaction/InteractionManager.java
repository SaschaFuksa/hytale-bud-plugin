package com.bud.interaction;

import java.util.Set;
import java.util.UUID;

import com.bud.llm.message.combat.LLMCombatManager;
import com.bud.llm.message.creation.Prompt;
import com.bud.BudConfig;
import com.bud.llm.ILLMChatManager;
import com.bud.llm.client.ILLMClient;
import com.bud.llm.client.LLMClientFactory;
import com.bud.npc.BudInstance;
import com.bud.npc.buds.sound.IBudSoundData;
import com.bud.result.ErrorResult;
import com.bud.result.IDataResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;

public class InteractionManager {

    private InteractionManager() {
        this.llmClient = LLMClientFactory.createClient();
    }

    private static final InteractionManager INSTANCE = new InteractionManager();

    private ILLMClient llmClient;

    private BudConfig config = BudConfig.getInstance();

    private final ChatInteraction chatInteraction = ChatInteraction.getInstance();

    private final SoundInteraction soundInteraction = SoundInteraction.getInstance();

    public static InteractionManager getInstance() {
        return INSTANCE;
    }

    public IResult processInteraction(Set<UUID> ownerIds, ILLMChatManager context) {
        for (UUID ownerId : ownerIds) {
            IResult result = processInteractionForOwner(ownerId, context);
            if (!result.isSuccess()) {
                LoggerUtil.getLogger().severe(() -> "Error processing interaction for owner " + ownerId + ": "
                        + result.getMessage());
            }
        }
        return new SuccessResult("Processed interactions for all owners.");
    }

    private IResult processInteractionForOwner(UUID ownerId, ILLMChatManager context) {
        BudInstance budInstance = context.getBudInstance(ownerId);
        if (budInstance == null) {
            return new SuccessResult("No bud available for owner " + ownerId);
        }
        try {
            String prompt = getPrompt(context, budInstance);
            if (prompt == null) {
                return new ErrorResult("No prompt for owner " + ownerId);
            } else if (prompt.equals(LLMCombatManager.NO_COMBAT_STRING)) {
                return new SuccessResult("No prompt for owner " + ownerId);
            }

            sendToChat(budInstance, prompt);
            return new SuccessResult("Triggered chat for owner " + ownerId);
        } catch (Exception e) {
            return new ErrorResult("Owner " + ownerId + ": Exception " + e.getMessage());
        }
    }

    private String getPrompt(ILLMChatManager context, BudInstance budInstance) {
        if (this.config.isEnableLLM()) {
            IDataResult<Prompt> promptResult = context.generatePrompt(budInstance);
            if (promptResult.isSuccess()) {
                return promptResult.getData().toString();
            } else {
                return context.getFallbackMessage(budInstance);
            }
        } else {
            return context.getFallbackMessage(budInstance);
        }
    }

    private void sendToChat(BudInstance budInstance, String prompt) {
        if (config.isEnableLLM()) {
            Thread.ofVirtual().start(() -> {
                try {
                    String response = this.llmClient.callLLM(prompt);
                    String message = budInstance.getData().getNPCDisplayName() + ": " + response;
                    LoggerUtil.getLogger().info(() -> "[BUD] LLM response: " + message);
                    this.chatInteraction.sendChatMessage(budInstance.getEntity().getWorld(), budInstance.getOwner(),
                            message);
                    playSound(budInstance);
                } catch (Exception e) {
                    LoggerUtil.getLogger().severe(() -> "[BUD] Random Chat Error: " + e.getMessage());
                }
            });
        } else {
            String message = budInstance.getData().getNPCDisplayName() + ": " + prompt;
            LoggerUtil.getLogger().info(() -> "[BUD] Fallback response: " + message);
            this.chatInteraction.sendChatMessage(budInstance.getEntity().getWorld(), budInstance.getOwner(),
                    message);
            playSound(budInstance);
        }
    }

    private void playSound(BudInstance budInstance) {
        IBudSoundData npcSoundData = budInstance.getData().getBudSoundData();
        if (npcSoundData != null) {
            String soundEventID = npcSoundData.getSoundForState("PetPassive");
            this.soundInteraction.playSound(budInstance.getEntity().getWorld(), budInstance.getEntity(),
                    soundEventID);
        }
    }

}
