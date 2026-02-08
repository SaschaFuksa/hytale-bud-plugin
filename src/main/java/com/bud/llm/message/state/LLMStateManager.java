package com.bud.llm.message.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.bud.llm.ILLMChatManager;
import com.bud.llm.message.creation.Prompt;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

public class LLMStateManager implements ILLMChatManager {

    private final LLMStateMessageCreation llmCreation;

    public LLMStateManager() {
        this.llmCreation = new LLMStateMessageCreation();
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance budInstance) {
        LLMStateContext contextResult = LLMStateContext.from(budInstance.getLastKnownState());
        Prompt prompt = this.llmCreation.createPrompt(contextResult, budInstance.getData().getBudMessage());
        return new DataResult<>(prompt, "Prompt generation.");
    }

    @Override
    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        Set<BudInstance> buds = BudRegistry.getInstance().getByOwner(ownerId);
        if (buds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<BudInstance> relevantBuds = ConcurrentHashMap.newKeySet();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (BudInstance budInstance : buds) {
            if (budInstance.getEntity() == null || budInstance.getRef() == null) {
                continue;
            }

            Store<EntityStore> store = budInstance.getRef().getStore();
            World world = store.getExternalData().getWorld();

            CompletableFuture<Void> future = new CompletableFuture<>();
            futures.add(future);

            world.execute(() -> {
                try {
                    String newState = checkStateChange(budInstance);
                    if (newState != null) {
                        relevantBuds.add(budInstance);
                    }
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return relevantBuds;
    }

    @Override
    public String getFallbackMessage(BudInstance budInstance) {
        return budInstance.getData().getBudMessage().getFallback(budInstance.getLastKnownState());
    }

    private String checkStateChange(BudInstance budInstance) {
        NPCEntity bud = budInstance.getEntity();
        Role role = bud.getRole();
        if (role == null) {
            return null;
        }

        String currentState = getMainStateName(role.getStateSupport().getStateName());
        String lastState = budInstance.getLastKnownState();

        if (lastState != null && lastState.equals(currentState)) {
            return null;
        }

        budInstance.setLastKnownState(currentState);
        return currentState;
    }

    /**
     * Extract and map state name from full state string (e.g.,
     * "PetDefensive.Default" -> "defensive")
     */
    public static String getMainStateName(String fullStateName) {
        if (fullStateName == null)
            return "unknown";

        String cleanName = fullStateName;
        int dotIndex = fullStateName.indexOf('.');
        if (dotIndex > 0) {
            cleanName = fullStateName.substring(0, dotIndex);
        }

        return switch (cleanName) {
            case "PetDefensive" -> "defensive";
            case "PetPassive" -> "passive";
            case "PetSitting" -> "stay";
            default -> "unknown";
        };
    }
}
