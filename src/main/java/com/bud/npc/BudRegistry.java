package com.bud.npc;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.bud.npc.npcdata.IBudNPCData;

public class BudRegistry {

    private static final BudRegistry INSTANCE = new BudRegistry();

    // Lookup by NPC Reference (Primary/Unique Key)
    private final Map<Ref<EntityStore>, BudInstance> byRef = new ConcurrentHashMap<>();

    // Lookup by Owner UUID (Secondary Key)
    private final Map<UUID, Set<BudInstance>> byOwner = new ConcurrentHashMap<>();

    private BudRegistry() {
    }

    public static BudRegistry getInstance() {
        return INSTANCE;
    }

    public void register(PlayerRef owner, NPCEntity entity, IBudNPCData data, String initialState) {
        Ref<EntityStore> ref = entity.getReference();
        if (ref == null)
            return;

        BudInstance instance = new BudInstance(owner, entity, data, initialState);
        byRef.put(ref, instance);

        byOwner.computeIfAbsent(owner.getUuid(), k -> ConcurrentHashMap.newKeySet()).add(instance);
    }

    public void unregister(NPCEntity entity) {
        Ref<EntityStore> ref = entity.getReference();
        if (ref == null)
            return;

        BudInstance instance = byRef.remove(ref);
        if (instance != null) {
            Set<BudInstance> ownerBuds = byOwner.get(instance.getOwner().getUuid());
            if (ownerBuds != null) {
                ownerBuds.remove(instance);
            }
        }
    }

    public BudInstance get(Ref<EntityStore> ref) {
        return byRef.get(ref);
    }

    public Set<BudInstance> getByOwner(UUID ownerId) {
        return byOwner.getOrDefault(ownerId, Collections.emptySet());
    }

    public Set<Ref<EntityStore>> getAllRefs() {
        return byRef.keySet();
    }

    public Set<UUID> getAllOwners() {
        return byOwner.keySet();
    }
}
