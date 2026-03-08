package com.bud.feature.chat.player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.bud.core.BudManager;
import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.types.BudType;
import com.bud.feature.LLMInteractionManager;
import com.bud.llm.interaction.LLMInteractionEntry;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class PlayerChatReactionHandler implements Consumer<PlayerChatEvent> {

    private static final String VERI = "veri";
    private static final String GRONKH = "gronkh";
    private static final String KEYLETH = "keyleth";

    @Override
    public void accept(PlayerChatEvent event) {
        String message = event.getContent().trim();
        if (message.isEmpty() || message.startsWith("/")) {
            return;
        }

        String username = event.getSender().getUsername();
        Ref<EntityStore> entityStoreRef = event.getSender().getReference();
        if (entityStoreRef == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] Could not get EntityStore for player: "
                    + username);
            return;
        }

        Store<EntityStore> entityStore = entityStoreRef.getStore();
        World world = entityStore.getExternalData().getWorld();

        world.execute(() -> this.handleChatReaction(entityStoreRef, message, username));
    }

    private void handleChatReaction(@Nonnull Ref<EntityStore> entityStoreRef, @Nonnull String message,
            @Nonnull String username) {
        Store<EntityStore> entityStore = entityStoreRef.getStore();
        PlayerBudComponent playerBudComponent = entityStore.getComponent(entityStoreRef,
                PlayerBudComponent.getComponentType());
        if (playerBudComponent == null || !playerBudComponent.hasBuds()) {
            return;
        }

        List<BudComponent> allBudComponents = getAllBudComponents(playerBudComponent);
        if (allBudComponents.isEmpty()) {
            return;
        }

        List<BudComponent> targetBudComponents = getMentionedBudComponents(message, allBudComponents);
        if (targetBudComponents.isEmpty()) {
            BudComponent budComponent = BudManager.getInstance().getRandomBudComponent(playerBudComponent);
            targetBudComponents = List.of(budComponent);
        }

        for (BudComponent budComponent : targetBudComponents) {
            if (budComponent == null) {
                continue;
            }
            PlayerChatEntry entry = new PlayerChatEntry(message, budComponent);
            Thread.ofVirtual().start(() -> {
                LLMInteractionManager.getInstance()
                        .processInteraction(new LLMInteractionEntry(LLMPlayerChatMessageCreation.getInstance(), entry));
            });
        }

        final int targetCount = targetBudComponents.size();
        LoggerUtil.getLogger().finer(() -> "[BUD] PlayerChat reaction processed for " + targetCount
                + " bud(s) from player " + username);
    }

    private static List<BudComponent> getAllBudComponents(@Nonnull PlayerBudComponent playerBudComponent) {
        List<BudComponent> budComponents = new ArrayList<>();
        for (NPCEntity bud : playerBudComponent.getCurrentBuds()) {
            BudComponent budComponent = BudManager.getInstance().findBudComponent(bud);
            if (budComponent != null) {
                budComponents.add(budComponent);
            }
        }
        return budComponents;
    }

    private static List<BudComponent> getMentionedBudComponents(@Nonnull String message,
            @Nonnull List<BudComponent> allBudComponents) {
        String lowerMessage = " " + message.toLowerCase() + " ";
        Set<BudType> mentionedTypes = new LinkedHashSet<>();

        if (containsWord(lowerMessage, VERI)) {
            mentionedTypes.add(BudType.VERI);
        }
        if (containsWord(lowerMessage, GRONKH)) {
            mentionedTypes.add(BudType.GRONKH);
        }
        if (containsWord(lowerMessage, KEYLETH)) {
            mentionedTypes.add(BudType.KEYLETH);
        }

        if (mentionedTypes.isEmpty()) {
            return List.of();
        }

        List<BudComponent> result = new ArrayList<>();
        for (BudComponent budComponent : allBudComponents) {
            if (mentionedTypes.contains(budComponent.getBudType())) {
                result.add(budComponent);
            }
        }
        return result;
    }

    private static boolean containsWord(@Nonnull String text, @Nonnull String word) {
        return text.matches(".*\\b" + java.util.regex.Pattern.quote(word) + "\\b.*");
    }
}
