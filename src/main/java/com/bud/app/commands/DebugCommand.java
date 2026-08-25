package com.bud.app.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;

import com.bud.core.components.BudComponent;
import com.bud.core.components.PlayerBudComponent;
import com.bud.core.registry.BudRegistry;
import com.bud.core.types.DayOfWeek;
import com.bud.core.types.Mood;
import com.bud.core.types.TimeOfDay;
import com.bud.feature.bud.MoodTracker;
import com.bud.feature.chat.ChatEvent;
import com.bud.feature.chat.conversation.ConversationMemoryEntry;
import com.bud.feature.chat.conversation.ConversationMemoryService;
import com.bud.feature.chat.conversation.DialogModeTracker;
import com.bud.feature.world.WorldInformationUtil;
import com.bud.feature.world.time.TimeInformationUtil;
import com.hypixel.hytale.builtin.hytalegenerator.LoggerUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.weather.config.Weather;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.zone.Zone;

public class DebugCommand extends AbstractPlayerCommand {

    private final FlagArg componentDataFlag;
    private final FlagArg moodFlag;
    private final FlagArg weatherFlag;
    private final FlagArg timeFlag;
    private final FlagArg worldFlag;
    private final FlagArg memoryFlag;
    private final FlagArg dialogFlag;
    private final FlagArg setMoodFlag;
    private final OptionalArg<String> setMoodBudArg;
    private final OptionalArg<String> setMoodValueArg;

    public DebugCommand() {
        super("debug", "Debug command for testing purposes.");
        this.componentDataFlag = this.withFlagArg("componentData",
                "Shows the current persisted data for the player's Buds.");
        this.moodFlag = this.withFlagArg("mood", "Shows the mood of the current Buds and their favorite day.");
        this.weatherFlag = this.withFlagArg("weather", "Shows the current weather.");
        this.timeFlag = this.withFlagArg("time", "Shows the current time of day and day of week.");
        this.worldFlag = this.withFlagArg("world", "Shows the current zone and biome.");
        this.memoryFlag = this.withFlagArg("memory", "Shows the current conversation memories.");
        this.dialogFlag = this.withFlagArg("dialog", "Triggers dialog mode immediately for your current Buds.");
        this.setMoodFlag = this.withFlagArg("setMood",
                "Force a Bud's mood and trigger the bud-to-bud mood-change reaction, same as an automatic change. "
                        + "Use with --budname and --moodname.");
        this.setMoodBudArg = Objects.requireNonNull(this.withOptionalArg("budname",
                "Bud to change (veri, gronkh, keyleth).", Objects.requireNonNull(ArgTypes.STRING)));
        this.setMoodValueArg = Objects.requireNonNull(this.withOptionalArg("moodname",
                "Mood to set (default, sad, insane, grumpy, dazed, overmotivated).",
                Objects.requireNonNull(ArgTypes.STRING)));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        boolean handled = false;

        if (this.componentDataFlag.get(context)) {
            handled = true;
            LoggerUtil.getLogger().fine(() -> "[BUD] Component debug command executed for player "
                    + playerRef.getUsername());
            this.sendComponentData(store, playerRef);
        }
        if (this.moodFlag.get(context)) {
            handled = true;
            this.sendMoodData(store, playerRef);
        }
        if (this.weatherFlag.get(context)) {
            handled = true;
            this.sendWeatherData(playerRef);
        }
        if (this.timeFlag.get(context)) {
            handled = true;
            this.sendTimeData(store, world, playerRef);
        }
        if (this.worldFlag.get(context)) {
            handled = true;
            this.sendWorldData(world, playerRef);
        }
        if (this.memoryFlag.get(context)) {
            handled = true;
            this.sendMemoryData(playerRef);
        }
        if (this.dialogFlag.get(context)) {
            handled = true;
            this.triggerDialogData(playerRef);
        }
        if (this.setMoodFlag.get(context)) {
            handled = true;
            this.setMood(store, playerRef, context.get(Objects.requireNonNull(this.setMoodBudArg)),
                    context.get(Objects.requireNonNull(this.setMoodValueArg)));
        }

        if (!handled) {
            ChatEvent.dispatch(playerRef, "Debug flags: --componentData, --mood, --weather, --time, --world, "
                    + "--memory, --dialog, --setMood --budname <bud> --moodname <mood>");
        }
    }

    private void sendComponentData(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef) {
        PlayerBudComponent playerBudComponent = getPlayerBudComponent(store, playerRef);
        if (playerBudComponent == null) {
            ChatEvent.dispatch(playerRef, "ComponentData: no PlayerBudComponent found.");
            return;
        }

        if (playerBudComponent.getCurrentBuds().isEmpty() && playerBudComponent.getBudIds().isEmpty()) {
            ChatEvent.dispatch(playerRef, "ComponentData: no Bud data found.");
            return;
        }

        for (NPCEntity bud : playerBudComponent.getCurrentBuds()) {
            ChatEvent.dispatch(playerRef, "ComponentData current: " + bud.getNPCTypeId());
        }
        playerBudComponent.getBudIds().forEach(budId -> ChatEvent.dispatch(playerRef,
                "ComponentData persisted: " + budId));
    }

    private void sendMoodData(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef) {
        PlayerBudComponent playerBudComponent = getPlayerBudComponent(store, playerRef);
        if (playerBudComponent == null || !playerBudComponent.hasBuds()) {
            ChatEvent.dispatch(playerRef, "Mood: no active Buds found.");
            return;
        }

        List<String> lines = new ArrayList<>();
        for (NPCEntity bud : playerBudComponent.getCurrentBuds()) {
            Ref<EntityStore> budRef = bud.getReference();
            if (budRef == null || !budRef.isValid()) {
                continue;
            }
            BudComponent budComponent = store.getComponent(budRef, BudComponent.getComponentType());
            if (budComponent == null) {
                continue;
            }
            String budDisplayName = BudRegistry.getInstance().get(budComponent.getBudId()).getDisplayName();
            DayOfWeek favoriteDay = BudRegistry.getInstance().get(budComponent.getBudId()).getFavoriteDay();
            lines.add("Mood " + budDisplayName + ": " + formatDisplayValue(budComponent.getCurrentMood().name())
                    + " (Favorite Day: " + formatDisplayValue(favoriteDay.name()) + ")");
        }

        if (lines.isEmpty()) {
            ChatEvent.dispatch(playerRef, "Mood: no active Buds found.");
            return;
        }

        for (String line : lines) {
            if (line == null) {
                continue;
            }
            ChatEvent.dispatch(playerRef, line);
        }
    }

    private void sendWeatherData(@Nonnull PlayerRef playerRef) {
        Weather weather = WorldInformationUtil.getCurrentWeather(playerRef);
        if (weather == null || weather.getId() == null || weather.getId().isBlank()) {
            ChatEvent.dispatch(playerRef, "Weather: Unknown");
            return;
        }
        ChatEvent.dispatch(playerRef, "Weather: " + formatDisplayValue(weather.getId()));
    }

    private void sendTimeData(@Nonnull Store<EntityStore> store, @Nonnull World world, @Nonnull PlayerRef playerRef) {
        TimeOfDay timeOfDay = TimeInformationUtil.getTimeOfDay(store);
        DayOfWeek dayOfWeek = TimeInformationUtil.getDayOfWeek(world);
        ChatEvent.dispatch(playerRef, "Time: " + formatDisplayValue(timeOfDay.name())
                + ", Day: " + formatDisplayValue(dayOfWeek.name()));
    }

    private void sendWorldData(@Nonnull World world, @Nonnull PlayerRef playerRef) {
        Vector3d position = playerRef.getTransform().getPosition();
        Zone zone = WorldInformationUtil.getCurrentZone(world, position);
        Biome biome = WorldInformationUtil.getCurrentBiome(world, position);

        String zoneName = zone != null ? formatDisplayValue(zone.name()) : "Unknown";
        String biomeName = biome != null ? formatDisplayValue(biome.getName()) : "Unknown";
        ChatEvent.dispatch(playerRef, "World: Zone " + zoneName + ", Biome " + biomeName);
    }

    private void sendMemoryData(@Nonnull PlayerRef playerRef) {
        List<ConversationMemoryEntry> memories = ConversationMemoryService.getInstance()
                .getMemoriesForOwner(playerRef.getUsername());
        if (memories.isEmpty()) {
            ChatEvent.dispatch(playerRef, "Memory: no current memories stored.");
            return;
        }

        int index = 1;
        for (ConversationMemoryEntry memory : memories) {
            ChatEvent.dispatch(playerRef, "Memory " + index + ": [" + formatDisplayValue(memory.mode().name())
                    + "] importance=" + memory.importance()
                    + ", score=" + String.format("%.2f", memory.effectiveScore())
                    + ", participants=" + String.join(", ", memory.participants())
                    + ", summary=" + memory.summary());
            index++;
        }
    }

    private void triggerDialogData(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> playerRefReference = playerRef.getReference();
        if (playerRefReference == null || !playerRefReference.isValid()) {
            ChatEvent.dispatch(playerRef, "Dialog: player reference is invalid.");
            return;
        }

        boolean triggered = DialogModeTracker.getInstance().triggerDialogNow(playerRefReference, playerRef);
        if (triggered) {
            ChatEvent.dispatch(playerRef, "Dialog: triggered immediate dialog mode turn.");
            return;
        }
        ChatEvent.dispatch(playerRef, "Dialog: could not trigger dialog mode. At least two active Buds are required.");
    }

    private void setMood(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef,
            @Nullable String budName, @Nullable String moodName) {
        if (budName == null || budName.isBlank() || moodName == null || moodName.isBlank()) {
            ChatEvent.dispatch(playerRef, "SetMood: needs --budname <veri|gronkh|keyleth> --moodname <mood>.");
            return;
        }
        Mood mood;
        try {
            mood = Mood.valueOf(moodName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            ChatEvent.dispatch(playerRef, "SetMood: unknown mood '" + moodName
                    + "'. Valid: default, sad, insane, grumpy, dazed, overmotivated.");
            return;
        }

        PlayerBudComponent playerBudComponent = getPlayerBudComponent(store, playerRef);
        if (playerBudComponent == null || !playerBudComponent.hasBuds()) {
            ChatEvent.dispatch(playerRef, "SetMood: no active Buds found.");
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
            ChatEvent.dispatch(playerRef, "SetMood: " + budDisplayName + " is now " + mood.getDisplayName() + ".");
            return;
        }
        ChatEvent.dispatch(playerRef, "SetMood: no active Bud named '" + budName + "' found.");
    }

    private PlayerBudComponent getPlayerBudComponent(@Nonnull Store<EntityStore> store, @Nonnull PlayerRef playerRef) {
        Ref<EntityStore> playerRefReference = playerRef.getReference();
        if (playerRefReference == null) {
            LoggerUtil.getLogger()
                    .severe(() -> "[BUD] PlayerRef reference is null for player " + playerRef.getUsername());
            return null;
        }

        PlayerBudComponent playerBudComponent = store.getComponent(playerRefReference,
                PlayerBudComponent.getComponentType());
        if (playerBudComponent == null) {
            LoggerUtil.getLogger().warning(() -> "[BUD] No PlayerBudComponent found for player "
                    + playerRef.getUsername());
        }
        return playerBudComponent;
    }

    private String formatDisplayValue(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }

        String normalized = value;
        int namespaceIndex = normalized.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex + 1 < normalized.length()) {
            normalized = normalized.substring(namespaceIndex + 1);
        }
        normalized = normalized.replace('-', '_');

        String[] parts = normalized.split("[_\\s]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase());
            }
        }
        return builder.length() > 0 ? builder.toString() : value;
    }

}
