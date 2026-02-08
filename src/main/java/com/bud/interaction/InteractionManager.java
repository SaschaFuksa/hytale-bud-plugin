package com.bud.interaction;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.bud.BudConfig;
import com.bud.llm.ILLMChatManager;
import com.bud.llm.client.ILLMClient;
import com.bud.llm.client.LLMClientFactory;
import com.bud.llm.message.combat.LLMCombatManager;
import com.bud.llm.message.creation.Prompt;
import com.bud.npc.BudInstance;
import com.bud.npc.buds.sound.IBudSoundData;
import com.bud.result.ErrorResult;
import com.bud.result.IDataResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.bud.util.WorldInformationUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.universe.world.World;

public class InteractionManager {

    private InteractionManager() {
        this.llmClient = LLMClientFactory.createClient();
    }

    private static final InteractionManager INSTANCE = new InteractionManager();

    private final ILLMClient llmClient;

    private final BudConfig config = BudConfig.getInstance();

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
        Set<BudInstance> budInstances = context.getRelevantBudInstances(ownerId);
        if (budInstances == null || budInstances.isEmpty()) {
            return new SuccessResult("No bud available for owner " + ownerId);
        }
        Set<BudInstance> errors = Collections.emptySet();
        for (BudInstance budInstance : budInstances) {
            try {
                Prompt prompt = getPrompt(context, budInstance);
                if (prompt == null) {
                    errors.add(budInstance);
                    LoggerUtil.getLogger().warning(() -> "[BUD] No prompt generated for owner " + ownerId);
                } else if (prompt.userPrompt().equals(LLMCombatManager.NO_COMBAT_STRING)) {
                    LoggerUtil.getLogger().info(() -> "[BUD] No combat prompt generated for owner " + ownerId);
                    continue;
                }

                sendToChat(budInstance, prompt, context);
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

    private Prompt getPrompt(ILLMChatManager context, BudInstance budInstance) {
        if (this.config.isEnableLLM()) {
            IDataResult<Prompt> promptResult = context.generatePrompt(budInstance);
            if (promptResult.isSuccess()) {
                return promptResult.getData();
            }
        }
        return null;
    }

    private void sendToChat(BudInstance budInstance, Prompt prompt, ILLMChatManager context) {
        World world = WorldInformationUtil.resolveWorld(budInstance);
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
                } catch (Exception e) {
                    LoggerUtil.getLogger().severe(() -> "[BUD] Random Chat Error: " + e.getMessage());
                }
            });
        } else {
            String message = budInstance.getData().getNPCDisplayName() + ": " + context.getFallbackMessage(budInstance);
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
