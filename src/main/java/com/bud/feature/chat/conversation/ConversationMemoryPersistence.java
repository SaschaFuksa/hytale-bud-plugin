package com.bud.feature.chat.conversation;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import com.bud.core.components.PlayerBudComponent;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

final class ConversationMemoryPersistence {

    private ConversationMemoryPersistence() {
    }

    static void persist(@Nonnull String normalizedOwnerKey, @Nonnull PlayerRef playerRef,
            @Nonnull List<ConversationMemoryEntry> regularMemories,
            @Nonnull Map<String, List<ConversationMemoryEntry>> legendaryBuckets) {
        try {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) {
                return;
            }
            Store<EntityStore> store = ref.getStore();

            Set<PersistedMemoryEntry> persistedMemories = new LinkedHashSet<>();
            for (var entry : regularMemories) {
                persistedMemories.add(PersistedMemoryEntry.from(Objects.requireNonNull(entry)));
            }

            Map<String, Set<PersistedMemoryEntry>> persistedLegendary = new HashMap<>();
            for (Map.Entry<String, List<ConversationMemoryEntry>> bucket : legendaryBuckets.entrySet()) {
                Set<PersistedMemoryEntry> converted = new LinkedHashSet<>();
                for (var entry : bucket.getValue()) {
                    converted.add(PersistedMemoryEntry.from(Objects.requireNonNull(entry)));
                }
                persistedLegendary.put(bucket.getKey(), converted);
            }

            store.getExternalData().getWorld().execute(() -> {
                PlayerBudComponent component = store.getComponent(ref, PlayerBudComponent.getComponentType());
                if (component == null) {
                    return;
                }
                component.setPersistedMemories(persistedMemories);
                component.setPersistedLegendaryMemories(persistedLegendary);
            });
        } catch (Exception exception) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Could not persist memories for " + normalizedOwnerKey
                    + ": " + exception.getMessage());
        }
    }

    @Nonnull
    static List<ConversationMemoryEntry> restoreRegularMemories(@Nonnull PlayerBudComponent component) {
        return Objects.requireNonNull(component.getPersistedMemories().stream()
                .map(entry -> entry.toEntry())
                .toList());
    }

    @Nonnull
    static Map<String, List<ConversationMemoryEntry>> restoreLegendaryBuckets(@Nonnull PlayerBudComponent component) {
        Map<String, List<ConversationMemoryEntry>> result = new HashMap<>();
        for (Map.Entry<String, Set<PersistedMemoryEntry>> entry : component.getPersistedLegendaryMemories()
                .entrySet()) {
            result.put(entry.getKey(), entry.getValue().stream().map(persisted -> persisted.toEntry()).toList());
        }
        return result;
    }
}
