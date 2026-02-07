package com.bud.npc;

import com.bud.npc.buds.IBudData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudInstance {
    private final PlayerRef owner;
    private final NPCEntity entity;
    private final IBudData data;
    private String lastKnownState;

    public BudInstance(PlayerRef owner, NPCEntity entity, IBudData data, String initialState) {
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

    public IBudData getData() {
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
