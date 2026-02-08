package com.bud.llm.message.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.llm.ILLMChatManager;
import com.bud.llm.message.creation.Prompt;
import com.bud.llm.message.prompt.LLMPromptManager;
import com.bud.npc.BudInstance;
import com.bud.npc.BudRegistry;
import com.bud.npc.buds.IBudData;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class LLMWorldManager implements ILLMChatManager {

    private final LLMWorldMessageCreation llmCreation;

    public LLMWorldManager() {
        this.llmCreation = new LLMWorldMessageCreation();
    }

    @Override
    public IDataResult<Prompt> generatePrompt(BudInstance instance) {
        IDataResult<LLMWorldContext> contextResult = createContext(instance);
        if (!contextResult.isSuccess()) {
            return new DataResult<>(null, "Failed to create context: " + contextResult.getMessage());
        }
        LLMWorldContext context = contextResult.getData();
        Prompt prompt = llmCreation.createPrompt(context, instance.getData().getBudMessage());
        return new DataResult<>(prompt, "Prompt generation.");
    }

    @Override
    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));
        if (ownerBuds.isEmpty())
            return null;

        return Set.of(ownerBuds.get((int) (Math.random() * ownerBuds.size())));
    }

    @Override
    public String getFallbackMessage(BudInstance budInstance) {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budName = budInstance.getData().getNPCDisplayName();
        return manager.getBudMessage(budName.toLowerCase()).getFallback("worldView");
    }

    private IDataResult<LLMWorldContext> createContext(BudInstance instance) {
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Generating world prompt for " + instance.getEntity().getNPCTypeId() + ".");
        PlayerRef owner = instance.getOwner();
        IBudData budNPCData = instance.getData();

        if (budNPCData == null)
            return new DataResult<>(null, "No NPC data available.");

        String npcName = budNPCData.getNPCDisplayName();
        LoggerUtil.getLogger().fine(() -> "[BUD] Current bud: " + npcName);

        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null)
            return new DataResult<>(null, "Owner EntityStore reference is null.");

        Store<EntityStore> store = ownerRef.getStore();
        World world = store.getExternalData().getWorld();
        LLMWorldContext context = LLMWorldContext.from(owner, world, store);

        LoggerUtil.getLogger().fine(() -> "[BUD] World data extracted: " + context.currentBiome().getName() + ", "
                + context.currentZone().name() + ", " + context.timeOfDay().name());
        return new DataResult<>(context, "Context created successfully.");
    }

}
