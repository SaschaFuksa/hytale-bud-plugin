package com.bud.core;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.role.Role;

public final class BudExecutionSupport {

    @FunctionalInterface
    public interface SupportAction {

        void accept(@Nonnull ExecutionSupport support);

    }

    @FunctionalInterface
    public interface SupportQuery<T> {

        @Nullable
        T apply(@Nonnull ExecutionSupport support);

    }

    private BudExecutionSupport() {
    }

    public static boolean with(@Nullable NPCEntity npc, @Nonnull SupportAction action) {
        return query(npc, support -> {
            action.accept(support);
            return Boolean.TRUE;
        }) != null;
    }

    @Nullable
    public static <T> T query(@Nullable NPCEntity npc, @Nonnull SupportQuery<T> mapper) {
        if (npc == null) {
            return null;
        }
        Role role = npc.getRole();
        if (role == null) {
            return null;
        }
        Ref<EntityStore> ref = npc.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        World world = npc.getWorld();
        if (world == null) {
            return null;
        }
        ComponentAccessor<EntityStore> accessor = Objects.requireNonNull(world.getEntityStore().getStore());
        ExecutionSupport support = role.acquireExecutionSupport(ref, accessor);
        try {
            return mapper.apply(support);
        } finally {
            support.clearForReuse();
        }
    }

}
