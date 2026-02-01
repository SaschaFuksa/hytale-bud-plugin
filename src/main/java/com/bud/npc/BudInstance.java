package com.bud.npc;

import com.bud.npc.npcdata.IBudNPCData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudInstance {
    private final PlayerRef owner;
    private final NPCEntity entity;
    private final IBudNPCData data;
    private String lastKnownState;

    public BudInstance(PlayerRef owner, NPCEntity entity, IBudNPCData data, String initialState) {
        this.owner = owner;
        this.entity = entity;
        this.data = data;
        this.lastKnownState = initialState;
    }

    public PlayerRef getOwner() {
        return owner;
    }

    public NPCEntity getEntity() {
        return entity;
    }

    public IBudNPCData getData() {
        return data;
    }

    public String getLastKnownState() {
        return lastKnownState;
    }

    public void setLastKnownState(String lastKnownState) {
        this.lastKnownState = lastKnownState;
    }

    public Ref<EntityStore> getRef() {
        return entity.getReference();
    }
}
