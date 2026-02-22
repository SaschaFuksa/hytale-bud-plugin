package com.bud.interaction;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.bud.config.LLMConfig;
import com.bud.llm.ILLMChatManager;
import com.bud.llm.client.ILLMClient;
import com.bud.llm.client.LLMClientFactory;
import com.bud.llm.message.Prompt;
import com.bud.npc.BudInstance;
import com.bud.profile.sound.IBudSoundData;
import com.bud.reaction.world.WorldInformationUtil;
import com.bud.result.ErrorResult;
import com.bud.result.IDataResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.universe.world.World;

public class InteractionManager {

    private InteractionManager() {
        this.llmClient = LLMClientFactory.createClient();
    }

    private static final InteractionManager INSTANCE = new InteractionManager();

    private final ILLMClient llmClient;

    private final LLMConfig config = LLMConfig.getInstance();

    private final ChatInteraction chatInteraction = ChatInteraction.getInstance();

    private final SoundInteraction soundInteraction = SoundInteraction.getInstance();

    public static InteractionManager getInstance() {
        return INSTANCE;
    }

    public IResult processInteraction(Set<UUID> ownerIds, ILLMChatManager chatManager) {
        for (UUID ownerId : ownerIds) {
            IResult result = processInteractionForOwner(ownerId, chatManager);
            if (!result.isSuccess()) {
                LoggerUtil.getLogger().severe(() -> "Error processing interaction for owner " + ownerId + ": "
                        + result.getMessage());
            }
        }
        return new SuccessResult("Processed interactions for all owners.");
    }

    private IResult processInteractionForOwner(UUID ownerId, ILLMChatManager chatManager) {
        Set<BudInstance> budInstances = chatManager.getRelevantBudInstances(ownerId);
        if (budInstances == null || budInstances.isEmpty()) {
            return new SuccessResult("No bud available for owner " + ownerId);
        }
        return processInteractionForBuds(budInstances, chatManager);
    }

    public IResult processInteractionForBuds(Set<BudInstance> budInstances, ILLMChatManager chatManager) {
        Set<BudInstance> errors = new HashSet<>();
        UUID ownerId = budInstances.iterator().next().getOwner().getUuid();
        for (BudInstance budInstance : budInstances) {
            try {
                Prompt prompt = getPrompt(chatManager, budInstance);
                if (prompt == null) {
                    LoggerUtil.getLogger().finer(() -> "[BUD] No prompt generated for owner " + ownerId);
                    continue;
                }

                sendToChat(budInstance, prompt);
                return new SuccessResult("Triggered chat for owner " + ownerId);
            } catch (Exception e) {
                errors.add(budInstance);
                LoggerUtil.getLogger().severe(
                        () -> "[BUD] Error processing interaction for owner " + ownerId + ": " + e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            return new ErrorResult("Errors processing some bud instances for owner " + ownerId);
        }
        return new SuccessResult("Processed interactions for owner " + ownerId);
    }

    private Prompt getPrompt(ILLMChatManager chatManager, BudInstance budInstance) {
        if (this.config.isEnableLLM()) {
            IDataResult<Prompt> promptResult = chatManager.generatePrompt(budInstance);
            if (promptResult.isSuccess()) {
                return promptResult.getData();
            }
        }
        String fallback = chatManager.getFallbackMessage(budInstance);
        if (fallback == null) {
            return null;
        }
        return new Prompt(fallback, fallback);
    }

    private void sendToChat(BudInstance budInstance, Prompt prompt) {
        World world = WorldInformationUtil.resolveWorld(budInstance.getOwner());
        if (world == null) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] Could not resolve world for bud " + budInstance.getData().getNPCDisplayName());
            return;
        }

        if (config.isEnableLLM()) {
            Thread.ofVirtual().start(() -> {
                try {
                    String response = this.llmClient.callLLM(prompt);
                    String message = budInstance.getData().getNPCDisplayName() + ": " + response;
                    LoggerUtil.getLogger().info(() -> "[BUD] LLM response: " + message);
                    this.chatInteraction.sendChatMessage(world, budInstance.getOwner(),
                            message);
                    playSound(budInstance, world);
                } catch (IOException | InterruptedException e) {
                    LoggerUtil.getLogger().severe(() -> "[BUD] Random Chat Error: " + e.getMessage());
                }
            });
        } else {
            String message = budInstance.getData().getNPCDisplayName() + ": " + prompt.userPrompt();
            LoggerUtil.getLogger().info(() -> "[BUD] Fallback response: " + message);
            this.chatInteraction.sendChatMessage(world, budInstance.getOwner(),
                    message);
            playSound(budInstance, world);
        }
    }

    private void playSound(BudInstance budInstance, World world) {
        IBudSoundData npcSoundData = budInstance.getData().getBudSoundData();
        if (npcSoundData != null) {
            String soundEventID = npcSoundData.getSoundForState("PetPassive");
            this.soundInteraction.playSound(world, budInstance.getEntity(),
                    soundEventID);
        }
    }

}
