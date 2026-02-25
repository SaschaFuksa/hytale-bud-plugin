package com.bud.old;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bud.feature.data.npc.BudInstance;
import com.bud.feature.data.npc.BudRegistry;
import com.bud.feature.profile.IBudProfile;
import com.bud.llm.messages.weather.LLMWeatherContext;
import com.bud.llm.prompt.LLMPromptManager;
import com.bud.llm.prompt.Prompt;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class LLMWorldManager {

    private final LLMWorldMessageCreation llmCreation;

    private final LLMWeatherContext weatherContext;

    public LLMWorldManager(String weatherId) {
        this.llmCreation = new LLMWorldMessageCreation();
        this.weatherContext = LLMWeatherContext.from(weatherId);
    }

    public Prompt generatePrompt(BudInstance instance) {
        LLMWorldContext context = createContext(instance);
        if (context == null) {
            return null;
        }
        Prompt prompt = llmCreation.createPrompt(context, instance);
        return prompt;
    }

    public Set<BudInstance> getRelevantBudInstances(UUID ownerId) {
        List<BudInstance> ownerBuds = new ArrayList<>(BudRegistry.getInstance().getByOwner(ownerId));
        if (ownerBuds.isEmpty())
            return null;

        return Set.of(ownerBuds.get((int) (Math.random() * ownerBuds.size())));
    }

    public String getFallbackMessage(BudInstance budInstance) {
        LLMPromptManager manager = LLMPromptManager.getInstance();
        String budName = budInstance.getData().getNPCDisplayName();
        return manager.getBudMessage(budName.toLowerCase()).getFallback("worldView");
    }

    private LLMWorldContext createContext(BudInstance instance) {
        LoggerUtil.getLogger()
                .fine(() -> "[BUD] Generating world prompt for " + instance.getEntity().getNPCTypeId() + ".");
        PlayerRef owner = instance.getOwner();
        IBudProfile budNPCData = instance.getData();

        if (budNPCData == null)
            return null;

        String npcName = budNPCData.getNPCDisplayName();
        LoggerUtil.getLogger().fine(() -> "[BUD] Current bud: " + npcName);

        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null)
            return null;

        Store<EntityStore> store = ownerRef.getStore();
        World world = store.getExternalData().getWorld();
        LLMWorldContext context = LLMWorldContext.from(owner, world, store, this.weatherContext);

        LoggerUtil.getLogger().fine(() -> "[BUD] World data extracted: " + context.currentBiome().getName() + ", "
                + context.currentZone().name() + ", " + context.timeOfDay().name());
        return context;
    }

}
