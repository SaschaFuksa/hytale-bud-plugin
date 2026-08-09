package com.bud.feature.queue.creation;

import java.util.Set;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record BudCreationEntry(@Nonnull Ref<EntityStore> playerRef, @Nonnull Set<String> budIds) {

}
