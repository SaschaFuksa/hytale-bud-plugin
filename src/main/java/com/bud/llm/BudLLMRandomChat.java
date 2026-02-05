package com.bud.llm;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.bud.interaction.BudChatInteraction;
import com.bud.interaction.BudSoundInteraction;
import com.bud.llm.llmclient.ILLMClient;
import com.bud.llm.llmclient.LLMClientFactory;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.npc.npcsound.IBudNPCSoundData;
import com.bud.result.ErrorResult;
import com.bud.result.IDataResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

public class BudLLMRandomChat {

    public static final String NO_COMBAT_STRING = "No recent combat interactions.";

    private static final BudLLMRandomChat INSTANCE = new BudLLMRandomChat();

    private BudLLMRandomChat() {
    }

    private ILLMClient llmClient;

    private final BudChatInteraction chatInteraction = BudChatInteraction.getInstance();

    private final BudSoundInteraction soundInteraction = BudSoundInteraction.getInstance();

    private ILLMClient getLlmClient() {
        if (llmClient == null) {
            llmClient = LLMClientFactory.createClient();
        }
        return llmClient;
    }

    public static BudLLMRandomChat getInstance() {
        return INSTANCE;
    }

    public IResult triggerRandomLLMChats(ILLMChatContext context) {
        // Iterate over all track owners
        Set<UUID> owners = BudRegistry.getInstance().getAllOwners();
        Set<String> errors = new HashSet<>();
        for (UUID ownerId : owners) {
            IResult result = triggerRandomLLMChats(ownerId, context);
            if (!result.isSuccess()) {
                errors.add(result.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            return new ErrorResult("Triggered random chats with errors: " + String.join("; ", errors));
        }
        return new SuccessResult("Triggered random chats for all bud owners.");
    }

    public IResult triggerRandomLLMChats(UUID ownerId, ILLMChatContext context) {
        BudInstance budInstance = context.getRandomInstanceForOwner(ownerId);
        if (budInstance == null) {
            return new SuccessResult("No bud available for owner " + ownerId);
        }
        try {
            IDataResult<String> result = context.generatePrompt(budInstance);
            if (!result.isSuccess()) {
                return new ErrorResult("Owner " + ownerId + ": " + result.getMessage());
            }
            String prompt = result.getData();
            if (prompt == null || prompt.equals(NO_COMBAT_STRING)) {
                return new SuccessResult("No prompt for owner " + ownerId);
            }
            interact(budInstance, prompt);
            return new SuccessResult("Triggered chat for owner " + ownerId);
        } catch (Exception e) {
            return new ErrorResult("Owner " + ownerId + ": Exception " + e.getMessage());
        }
    }

    private void playSound(BudInstance budInstance) {
        IBudNPCSoundData npcSoundData = budInstance.getData().getBudNPCSoundData();
        if (npcSoundData != null) {
            String soundEventID = npcSoundData.getSoundForState("PetPassive");
            this.soundInteraction.playSound(budInstance.getEntity().getWorld(), budInstance.getEntity(),
                    soundEventID);
        }
    }

    private void interact(BudInstance budInstance, String prompt) {
        Thread.ofVirtual().start(() -> {
            try {
                String response = getLlmClient().callLLM(prompt);
                String message = budInstance.getData().getNPCDisplayName() + ": " + response;
                LoggerUtil.getLogger().info(() -> "[BUD] LLM response: " + message);
                this.chatInteraction.sendChatMessage(budInstance.getEntity().getWorld(), budInstance.getOwner(),
                        message);
                playSound(budInstance);
            } catch (Exception e) {
                LoggerUtil.getLogger().severe(() -> "[BUD] Random Chat Error: " + e.getMessage());
            }
        });
    }

}
