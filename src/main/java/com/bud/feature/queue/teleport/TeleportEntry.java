package com.bud.feature.queue.teleport;

import java.util.Set;

import javax.annotation.Nonnull;

import org.jspecify.annotations.NonNull;

import com.bud.core.components.PlayerBudComponent;
import com.bud.feature.profile.BudType;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.bud.feature.queue.IQueueEntry;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record TeleportEntry(@NonNull PlayerBudComponent playerBudComponent,
        @NonNull Set<BudType> budTypes, @Nonnull Store<EntityStore> store,
        @NonNull LLMInteractionEntry interactionEntry)
        implements IQueueEntry {

    @Override
    public int getPriority() {
        return 3;
    }

    @Override
    public LLMInteractionEntry getInteractionEntry() {
        return interactionEntry;
    }

}
