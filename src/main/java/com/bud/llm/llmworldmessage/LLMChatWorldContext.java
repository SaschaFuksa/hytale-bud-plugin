package com.bud.llm.llmworldmessage;

import com.bud.llm.ILLMChatContext;
import com.bud.llm.llmmessage.ILLMBudNPCMessage;
import com.bud.npc.BudInstance;
import com.bud.npc.npcdata.IBudNPCData;
import com.bud.result.DataResult;
import com.bud.result.IDataResult;
import com.bud.system.BudTimeInformation;
import com.bud.system.BudWorldContext;
import com.bud.system.BudWorldInformation;
import com.bud.system.TimeOfDay;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.Zone;

public class LLMChatWorldContext implements ILLMChatContext {

    @Override
    public IDataResult<String> generatePrompt(BudInstance instance) {
        PlayerRef owner = instance.getOwner();
        IBudNPCData budNPCData = instance.getData();

        if (budNPCData == null)
            return new DataResult<>(null, "No NPC data available.");

        ILLMBudNPCMessage npcMessage = budNPCData.getLLMBudNPCMessage();
        String npcName = budNPCData.getNPCDisplayName();
        System.out.println("[BUD] current bud: " + npcName);

        System.out.println("[BUD] Start extracting world data.");
        Ref<EntityStore> ownerRef = owner.getReference();
        if (ownerRef == null)
            return new DataResult<>(null, "Owner EntityStore reference is null.");

        Store<EntityStore> store = ownerRef.getStore();
        World world = store.getExternalData().getWorld();
        BudWorldContext context = getWorldContext(owner, world, store);
        System.out.println("[BUD] World data extracted: " + context.currentBiome().getName() + ", "
                + context.currentZone().name() + ", " + context.timeOfDay().name());

        System.out.println("[BUD] Preparing Sound.");
        String prompt = LLMWorldInfoMessageManager.createPrompt(context, npcMessage);
        return new DataResult<>(prompt, "Prompt generated successfully.");
    }

    private BudWorldContext getWorldContext(PlayerRef owner, World world, Store<EntityStore> store) {
        Vector3d pos = owner.getTransform().getPosition();
        TimeOfDay timeOfDay = BudTimeInformation.getTimeOfDay(store);
        System.out.println("[BUD] time of day: " + timeOfDay.name());
        Biome currentBiome = BudWorldInformation.getCurrentBiome(world, pos);
        System.out.println("[BUD] current biome: " + currentBiome.getName());
        Zone currentZone = BudWorldInformation.getCurrentZone(world, pos);
        System.out.println("[BUD] current zone: " + currentZone.name());
        return new BudWorldContext(timeOfDay, currentZone, currentBiome);
    }

}
