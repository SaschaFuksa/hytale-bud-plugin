package com.bud.llm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.interaction.BudChatInteraction;
import com.bud.interaction.BudSoundInteraction;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.npc.npcsound.IBudNPCSoundData;
import com.bud.result.ErrorResult;
import com.bud.result.IDataResult;
import com.bud.result.IResult;
import com.bud.result.SuccessResult;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

public class BudLLMRandomChat {

    public static final String NO_COMBAT_STRING = "No recent combat interactions.";

    private static final BudLLMRandomChat INSTANCE = new BudLLMRandomChat();

    private BudLLMRandomChat() {
    }

    private final BudLLM budLLM = new BudLLM();

    private final BudChatInteraction chatInteraction = new BudChatInteraction();

    private final BudSoundInteraction soundInteraction = new BudSoundInteraction();

    public static BudLLMRandomChat getInstance() {
        return INSTANCE;
    }

    public IResult triggerRandomLLMChats(ILLMChatContext context) {
        // Iterate over all track owners
        Set<UUID> owners = BudRegistry.getInstance().getAllOwners();
        Set<String> errors = new HashSet<>();
        for (UUID ownerId : owners) {
            BudInstance budInstance = getRandomInstanceForOwner(ownerId);
            if (budInstance == null)
                continue;
            try {
                IDataResult<String> result = context.generatePrompt(budInstance);
                if (!result.isSuccess()) {
                    errors.add("Owner " + ownerId + ": " + result.getMessage());
                } else {
                    String prompt = result.getData();
                    if (prompt.equals(NO_COMBAT_STRING)) {
                        continue;
                    }
                    playSound(budInstance);
                    sendToChat(
                            budInstance.getData().getNPCDisplayName(),
                            prompt,
                            budInstance.getOwner(),
                            budInstance.getEntity().getWorld());
                }
            } catch (Exception e) {
                errors.add("Owner " + ownerId + ": Exception " + e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            return new ErrorResult("Triggered random chats with errors: " + String.join("; ", errors));
        }
        return new SuccessResult("Triggered random chats for all bud owners.");
    }

    private BudInstance getRandomInstanceForOwner(UUID ownerId) {
        List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));
        if (ownerBuds.isEmpty())
            return null;

        return ownerBuds.get((int) (Math.random() * ownerBuds.size()));
    }

    private void playSound(BudInstance budInstance) {
        IBudNPCSoundData npcSoundData = budInstance.getData().getBudNPCSoundData();
        if (npcSoundData != null) {
            String soundEventID = npcSoundData.getSoundForState("PetPassive");
            this.soundInteraction.playSound(budInstance.getEntity().getWorld(), budInstance.getEntity(),
                    soundEventID);
        }
    }

    private void sendToChat(String npcName, String prompt, PlayerRef owner, World world) {
        Thread.ofVirtual().start(() -> {
            try {
                String response = budLLM.callLLM(prompt);
                String message = npcName + ": " + response;
                System.out.println("[BUD] LLM response: " + message);
                this.chatInteraction.sendChatMessage(world, owner, message);
            } catch (Exception e) {
                System.out.println("[BUD] Random Chat Error: " + e.getMessage());
            }
        });
    }

}
