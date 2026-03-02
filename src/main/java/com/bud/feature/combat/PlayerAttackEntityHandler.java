package com.bud.feature.combat;

import java.util.function.Consumer;

import com.bud.core.components.PlayerBudComponent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport;

public class PlayerAttackEntityHandler implements Consumer<PlayerAttackEntityEvent> {

    @Override
    public void accept(PlayerAttackEntityEvent event) {
        LoggerUtil.getLogger().fine(() -> "[BUD] PlayerAttackEntityEvent received: playerRef=" + event.playerRef()
                + ", targetRef=" + event.targetRef());
        if (!event.playerRef().isValid() || !event.targetRef().isValid()) {
            return;
        }

        Store<EntityStore> store = event.store();
        PlayerBudComponent playerBudComponent = store.getComponent(event.playerRef(),
                PlayerBudComponent.getComponentType());
        if (playerBudComponent == null || !playerBudComponent.hasBuds()) {
            return;
        }

        for (NPCEntity bud : playerBudComponent.getCurrentBuds()) {
            if (bud == null) {
                continue;
            }

            Ref<EntityStore> budRef = bud.getReference();
            if (budRef == null || !budRef.isValid()) {
                continue;
            }
            ComponentType<EntityStore, NPCEntity> npcEntityComponentType = NPCEntity.getComponentType();
            if (npcEntityComponentType == null) {
                continue;
            }
            NPCEntity npcEntity = store.getComponent(budRef, npcEntityComponentType);
            if (npcEntity == null) {
                continue;
            }

            Role role = npcEntity.getRole();
            if (role == null) {
                continue;
            }

            MarkedEntitySupport markedEntitySupport = role.getMarkedEntitySupport();

            markedEntitySupport.setMarkedEntity(MarkedEntitySupport.DEFAULT_TARGET_SLOT, event.targetRef());
            markedEntitySupport.setMarkedEntity(0, event.targetRef());
        }
        LoggerUtil.getLogger().fine(() -> "[BUD] PlayerAttackEntityEvent processed for playerRef=" + event.playerRef()
                + ", targetRef=" + event.targetRef());
    }

}