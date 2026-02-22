package com.bud.reaction.state;

import javax.annotation.Nonnull;

import com.bud.components.BudComponent;
import com.bud.queue.state.StateChangeEntry;
import com.bud.queue.state.StateChangeQueue;
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
        Role role = budComponent.getBud().getRole();
        if (role == null) {
            return;
        }
        String currentStateName = role.getStateSupport().getStateName().split("\\.")[0];
        if (!currentStateName.equals("Idle")
                && !currentStateName.equals(budComponent.getCurrentState().getStateName())) {
            LoggerUtil.getLogger()
                    .fine(() -> "[BUD] State change detected for NPC \"" + budComponent.getBud().getNPCTypeId()
                            + "\". Old State: " + budComponent.getCurrentState().getStateName() + ", New State: "
                            + currentStateName);
            BudState newState = BudState.fromStateName(currentStateName);
            if (newState == null) {
                LoggerUtil.getLogger()
                        .warning(() -> "[BUD] Unrecognized state \"" + currentStateName
                                + "\" for NPC \"" + budComponent.getBud().getNPCTypeId()
                                + "\". Skipping state change.");
                return;
            }
            budComponent.setCurrentState(newState);
            StateChangeQueue.getInstance()
                    .addToCache(new StateChangeEntry(budComponent, newState));
        }
    }

}
