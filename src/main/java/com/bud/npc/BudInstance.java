package com.bud.npc;

import com.bud.profile.IBudProfile;
import com.bud.reaction.world.time.Mood;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class BudInstance {
    private final PlayerRef owner;
    private NPCEntity entity;
    private final IBudProfile data;
    private String lastKnownState;
    private Mood currentMood;

    public BudInstance(PlayerRef owner, NPCEntity entity, IBudProfile data, String initialState) {
        this.owner = owner;
        this.entity = entity;
        this.data = data;
        this.lastKnownState = initialState;
        this.currentMood = Mood.DEFAULT;
    }

    public PlayerRef getOwner() {
        return owner;
    }

    public NPCEntity getEntity() {
        return entity;
    }

    public IBudProfile getData() {
        return data;
    }

    public String getLastKnownState() {
        return lastKnownState;
    }

    public void setLastKnownState(String lastKnownState) {
        this.lastKnownState = lastKnownState;
    }

    public Mood getCurrentMood() {
        return currentMood;
    }

    public void setCurrentMood(Mood currentMood) {
        this.currentMood = currentMood;
    }

    public void setEntity(NPCEntity entity) {
        this.entity = entity;
    }

    public Ref<EntityStore> getRef() {
        return entity.getReference();
    }
}
