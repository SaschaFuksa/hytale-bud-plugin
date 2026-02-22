package com.bud.reaction.state;

import javax.annotation.Nonnull;

import com.bud.components.BudComponent;
import com.bud.events.ChatEvent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;

public class BudStateChangeSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(BudComponent.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        BudComponent budComponent = archetypeChunk.getComponent(index, BudComponent.getComponentType());
        if (budComponent == null) {
            return;
        }
        if (budComponent.getBud() == null) {
            // TODO: Cleanup
            return;
        }
        Role role = budComponent.getBud().getRole();
        if (role == null) {
            return;
        }
        String currentStateName = role.getStateSupport().getStateName().split("\\.")[0];
        if (!currentStateName.equals(budComponent.getCurrentStateName())) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] State change detected for NPC " + budComponent.getBud().getNPCTypeId());
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] Old State: " + budComponent.getCurrentStateName() + ", New State: "
                            + currentStateName);
            budComponent.setCurrentStateName(currentStateName);
            // Trigger LLM -> then ChatEvent
            ChatEvent.dispatch(budComponent.getPlayerRef(), currentStateName);
        }
    }

}
