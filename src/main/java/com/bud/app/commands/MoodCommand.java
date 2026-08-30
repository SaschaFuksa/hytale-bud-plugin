package com.bud.app.commands;

import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.registry.BudRegistry;
import com.bud.core.types.Mood;
import com.bud.feature.bud.MoodTracker;
import com.bud.feature.chat.ChatEvent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

public class MoodCommand extends AbstractPlayerCommand {

    @Nonnull
    private final RequiredArg<String> budArg;

    @Nonnull
    private final RequiredArg<String> moodArg;

    public MoodCommand() {
        super("mood", "Forces a Bud's mood and triggers the bud-to-bud mood-change reaction, same as an "
                + "automatic change.");
        this.requireNoPermission();
        this.budArg = Objects.requireNonNull(this.withRequiredArg("bud", "Bud to change (veri, gronkh, keyleth).",
                Objects.requireNonNull(ArgTypes.STRING)));
        this.moodArg = Objects.requireNonNull(this.withRequiredArg("mood",
                "Mood to set (default, sad, insane, grumpy, dazed, overmotivated).",
                Objects.requireNonNull(ArgTypes.STRING)));
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        String budName = Objects.requireNonNull(context.get(this.budArg));
        String moodName = Objects.requireNonNull(context.get(this.moodArg));

        Mood mood;
        try {
            mood = Mood.valueOf(moodName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            ChatEvent.dispatch(playerRef, "Mood: unknown mood '" + moodName
                    + "'. Valid: default, sad, insane, grumpy, dazed, overmotivated.");
            return;
        }

        Ref<EntityStore> playerRefReference = playerRef.getReference();
        if (playerRefReference == null) {
            ChatEvent.dispatch(playerRef, "Mood: player reference is invalid.");
            return;
        }
        PlayerBudComponent playerBudComponent = store.getComponent(playerRefReference,
                PlayerBudComponent.getComponentType());
        if (playerBudComponent == null || !playerBudComponent.hasBuds()) {
            ChatEvent.dispatch(playerRef, "Mood: no active Buds found.");
            return;
        }

        String normalizedBudId = BudRegistry.normalize(budName);
        for (NPCEntity bud : playerBudComponent.getCurrentBuds()) {
            Ref<EntityStore> budRef = bud.getReference();
            if (budRef == null || !budRef.isValid()) {
                continue;
            }
            BudComponent budComponent = store.getComponent(budRef, BudComponent.getComponentType());
            if (budComponent == null || !budComponent.getBudId().equals(normalizedBudId)) {
                continue;
            }
            MoodTracker.getInstance().forceMood(budComponent, mood);
            String budDisplayName = BudRegistry.getInstance().get(budComponent.getBudId()).getDisplayName();
            ChatEvent.dispatch(playerRef, "Mood: " + budDisplayName + " is now " + mood.getDisplayName() + ".");
            return;
        }
        ChatEvent.dispatch(playerRef, "Mood: no active Bud named '" + budName + "' found.");
    }

}
